package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowMilestoneRequest;
import com.rit.performance.dto.request.SowDocumentRequest;
import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.SowMapper;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SowServiceImpl implements SowService {
    private final SowRepository sowRepository;
    private final SowMilestoneRepository milestoneRepository;
    private final LookupValueRepository lookupValueRepository;
    private final EmployeeRepository employeeRepository;
    private final CsxEmployeeRepository csxEmployeeRepository;
    private final DocumentRepository documentRepository;

    @Override
    public SowResponse create(SowRequest request) {
        validateRequest(request);
        String code = normalizeCode(request.getSowCode());
        validateUniqueCode(code, null);

        Sow sow = new Sow();
        sow.setSowCode(code);
        applySowFields(sow, request);
        sowRepository.saveAndFlush(sow);

        MilestoneSync milestoneSync = synchronizeMilestones(sow, request.getMilestones());
        milestoneRepository.saveAll(milestoneSync.retained());
        milestoneRepository.flush();
        return toResponse(sowRepository.saveAndFlush(sow));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowResponse> getAll() {
        List<Sow> sows = sowRepository.findAllWithDetails();
        Map<Long, CsxEmployee> csxEmployees = csxEmployeesFor(sows);
        return sows.stream()
                .sorted(Comparator.comparing(
                        Sow::getUpdatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(sow -> SowMapper.toResponse(sow, csxEmployees))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SowResponse getById(Long id) {
        return toResponse(findSow(id));
    }

    @Override
    public SowResponse update(Long id, SowRequest request) {
        validateRequest(request);
        Sow sow = findSow(id);
        String code = normalizeCode(request.getSowCode());
        validateUniqueCode(code, id);
        sow.setSowCode(code);
        applySowFields(sow, request);

        MilestoneSync milestoneSync = synchronizeMilestones(sow, request.getMilestones());
        milestoneRepository.saveAll(milestoneSync.retained());
        milestoneRepository.flush();

        for (SowMilestone obsolete : milestoneSync.obsolete()) {
            sow.removeMilestone(obsolete);
        }
        return toResponse(sowRepository.saveAndFlush(sow));
    }

    @Override
    public void delete(Long id) {
        sowRepository.delete(findSow(id));
    }

    private MilestoneSync synchronizeMilestones(
            Sow sow,
            List<SowMilestoneRequest> milestoneRequests
    ) {
        List<SowMilestoneRequest> requests =
                milestoneRequests == null ? List.of() : milestoneRequests;
        Map<Long, SowMilestone> existing = sow.getMilestones().stream()
                .filter(milestone -> milestone.getId() != null)
                .collect(Collectors.toMap(SowMilestone::getId, Function.identity()));
        Set<Long> requestedIds = new HashSet<>();
        Set<SowMilestone> retained = new LinkedHashSet<>();

        for (SowMilestoneRequest request : requests) {
            validateDateRange(request.getStartDate(), request.getEndDate(), "Milestone");
            SowMilestone milestone;
            if (request.getId() == null) {
                milestone = new SowMilestone();
                sow.addMilestone(milestone);
            } else {
                if (!requestedIds.add(request.getId())) {
                    throw new InvalidOperationException(
                            "Duplicate milestone id in request: " + request.getId());
                }
                milestone = existing.get(request.getId());
                if (milestone == null) {
                    throw new InvalidOperationException(
                            "Milestone " + request.getId() + " does not belong to SOW " + sow.getId());
                }
            }
            applyMilestoneFields(milestone, request);
            retained.add(milestone);
        }

        Set<SowMilestone> obsolete = new LinkedHashSet<>(sow.getMilestones());
        obsolete.removeAll(retained);
        return new MilestoneSync(retained, obsolete);
    }

    private void applySowFields(Sow sow, SowRequest request) {
        sow.setSowName(request.getSowName().trim());
        sow.setBusinessUnit(lookupValueRepository.findById(request.getBusinessUnitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business unit not found: " + request.getBusinessUnitId())));
        sow.setSubmittedDate(request.getSubmittedDate());
        sow.setCsxProjectId(trimToNull(request.getCsxProjectId()));
        validateCsxEmployee(request.getCsxContactEmployeeId(), "CSX contact");
        validateCsxEmployee(request.getCsxEscalationEmployeeId(), "CSX escalation contact");
        sow.setCsxContactEmployeeId(request.getCsxContactEmployeeId());
        sow.setCsxEscalationEmployeeId(request.getCsxEscalationEmployeeId());
        sow.setRitContactEmployee(findRitEmployee(request.getRitContactEmployeeId()));
        sow.setStartDate(request.getStartDate());
        sow.setEndDate(request.getEndDate());
        sow.setStatus(normalizeStatus(request.getStatus(), "DRAFT"));
        if (request.getDocumentList() != null) {
            synchronizeDocuments(sow, request.getDocumentList());
        }
    }

    private void applyMilestoneFields(SowMilestone milestone, SowMilestoneRequest request) {
        milestone.setMilestoneName(request.getMilestoneName().trim());
        milestone.setStartDate(request.getStartDate());
        milestone.setEndDate(request.getEndDate());
        milestone.setInvoiceDate(request.getInvoiceDate());
        milestone.setAmount(request.getAmount());
        milestone.setStatus(normalizeStatus(request.getStatus(), "NOT_STARTED"));
    }

    private void validateRequest(SowRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate(), "SOW");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String label) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidOperationException(
                    label + " endDate cannot be before startDate");
        }
    }

    private Sow findSow(Long id) {
        return sowRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + id));
    }

    private Employee findRitEmployee(Long id) {
        if (id == null) return null;
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RIT contact employee not found: " + id));
    }

    private void validateCsxEmployee(Long id, String label) {
        if (id != null && !csxEmployeeRepository.existsById(id)) {
            throw new ResourceNotFoundException(label + " employee not found: " + id);
        }
    }

    private void synchronizeDocuments(Sow sow, List<SowDocumentRequest> documentList) {
        List<Long> requestedIds = documentList.stream()
                .map(SowDocumentRequest::getId)
                .toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidOperationException(
                    "documentList cannot contain duplicate document ids");
        }

        List<Document> documents = uniqueIds.isEmpty()
                ? List.of() : documentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(uniqueIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Documents not found: " + missingIds);
        }

        sow.getDocuments().clear();
        sow.getDocuments().addAll(documents);
    }

    private SowResponse toResponse(Sow sow) {
        return SowMapper.toResponse(sow, csxEmployeesFor(List.of(sow)));
    }

    private Map<Long, CsxEmployee> csxEmployeesFor(List<Sow> sows) {
        Set<Long> employeeIds = sows.stream()
                .flatMap(sow -> java.util.stream.Stream.of(
                        sow.getCsxContactEmployeeId(),
                        sow.getCsxEscalationEmployeeId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (employeeIds.isEmpty()) return Map.of();
        return csxEmployeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(CsxEmployee::getId, Function.identity()));
    }

    private void validateUniqueCode(String code, Long currentId) {
        boolean exists = currentId == null
                ? sowRepository.existsBySowCodeIgnoreCase(code)
                : sowRepository.existsBySowCodeIgnoreCaseAndIdNot(code, currentId);
        if (exists) throw new DuplicateResourceException("SOW code already exists: " + code);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String value, String defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 30) {
            throw new InvalidOperationException("status must not exceed 30 characters");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record MilestoneSync(
            Set<SowMilestone> retained,
            Set<SowMilestone> obsolete
    ) {
    }
}
