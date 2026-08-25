package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowMilestoneRequest;
import com.rit.performance.dto.request.SowDocumentRequest;
import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.request.SowAssignmentUpdateRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.dto.response.SowAssignmentResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.SowMapper;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowInvoiceService;
import com.rit.performance.service.SowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SowServiceImpl implements SowService {
    private final SowRepository sowRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowMilestoneRepository milestoneRepository;
    private final SowInvoiceService sowInvoiceService;
    private final SowFeatureRepository featureRepository;
    private final LookupValueRepository lookupValueRepository;
    private final RateCardRepository rateCardRepository;
    private final SowMilestonePositionAssignmentRepository positionAssignmentRepository;
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
        createDraftInvoicesWhenEligible(sow, milestoneSync.retained());
        return toResponse(sowRepository.saveAndFlush(sow));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowResponse> getAll() {
        return getAll(null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowResponse> getAll(Long sowId, String status, Long designationId) {
        String normalizedStatus = trimToNull(status);
        List<Sow> sows = sowRepository.findAllWithDetails().stream()
                .filter(sow -> sowId == null || Objects.equals(sow.getId(), sowId))
                .filter(sow -> normalizedStatus == null
                        || normalizedStatus.equalsIgnoreCase(sow.getStatus()))
                .filter(sow -> designationId == null || sow.getMilestones().stream()
                        .flatMap(milestone -> milestone.getPositions().stream())
                        .anyMatch(position -> position.getPosition() != null
                                && Objects.equals(position.getPosition().getId(), designationId)))
                .toList();
        Map<Long, CsxEmployee> csxEmployees = csxEmployeesFor(sows);
        List<SowResponse> responses = sows.stream()
                .sorted(Comparator.comparing(
                        Sow::getUpdatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(sow -> SowMapper.toResponse(sow, csxEmployees))
                .toList();
        populateResourceFulfillment(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public SowResponse getById(Long id) {
        return toResponse(findSow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowAssignmentResponse> getAllAssignments() {
        Map<Long, Sow> sows = sowRepository.findAllWithDetails().stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        List<EmployeeAssignment> assignments = assignmentRepository
                .findByStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDesc("ACTIVE").stream()
                .filter(assignment -> assignment.getSowId() != null
                        && sows.containsKey(assignment.getSowId()))
                .toList();
        return assignmentResponses(assignments, sows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowAssignmentResponse> getAssignments(Long sowId) {
        Sow sow = findSow(sowId);
        List<EmployeeAssignment> assignments = assignmentRepository
                .findBySowIdAndStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDescIdDesc(
                        sowId, "ACTIVE");
        return assignmentResponses(assignments, Map.of(sowId, sow));
    }

    @Override
    public SowAssignmentResponse updateAssignment(
            Long assignmentId, SowAssignmentUpdateRequest request) {
        EmployeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee assignment not found: " + assignmentId));
        if (assignment.getSowId() == null) {
            throw new InvalidOperationException("Assignment is not linked to a SOW");
        }
        Sow sow = findSow(assignment.getSowId());
        Employee employee = requireEmployee(assignment.getEmployeeId(), "Employee");
        if (request.getMilestoneId() != null) {
            milestoneRepository.findByIdAndSow_Id(request.getMilestoneId(), sow.getId())
                    .orElseThrow(() -> new InvalidOperationException(
                            "Milestone " + request.getMilestoneId()
                                    + " does not belong to SOW " + sow.getId()));
        }
        lookupValueRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Designation not found: " + request.getDesignationId()));
        validateSupervisor(request.getLeadId(), employee.getId(), "Team Lead");
        validateSupervisor(request.getManagerId(), employee.getId(), "Manager");
        validateAssignmentDates(request.getAssignmentStartDate(), request.getAssignmentEndDate());
        String status = normalizeAssignmentStatus(request.getAssignmentStatus());
        if ("COMPLETED".equals(status) && request.getAssignmentEndDate() == null) {
            throw new InvalidOperationException(
                    "assignmentEndDate is required when assignmentStatus is COMPLETED");
        }
        if ("ACTIVE".equals(status)) {
            if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
                throw new InvalidOperationException("Employee is not active: " + employee.getId());
            }
            if (assignmentRepository.existsBySowIdAndEmployeeIdAndStatusIgnoreCaseAndIdNot(
                    sow.getId(), employee.getId(), "ACTIVE", assignmentId)) {
                throw new DuplicateResourceException(
                        "Employee " + employee.getId() + " already has an active assignment for SOW "
                                + sow.getId());
            }
            if (Boolean.TRUE.equals(request.getIsPrimaryAssignment())) {
                clearPrimaryAssignment(employee.getId(), assignmentId, request.getUpdatedBy());
            }
        }

        assignment.setMilestoneId(request.getMilestoneId());
        assignment.setDesignationId(request.getDesignationId());
        assignment.setPositionType(normalizePositionType(request.getPositionType()));
        assignment.setLeadId(request.getLeadId());
        assignment.setManagerId(request.getManagerId());
        assignment.setAllocationPercentage(request.getAllocationPercentage());
        assignment.setIsPrimaryAssignment("ACTIVE".equals(status)
                && Boolean.TRUE.equals(request.getIsPrimaryAssignment()));
        assignment.setEffectiveFrom(request.getAssignmentStartDate());
        assignment.setEffectiveTo(request.getAssignmentEndDate());
        assignment.setStatus(status);
        assignment.setUpdatedBy(request.getUpdatedBy());
        EmployeeAssignment saved = assignmentRepository.save(assignment);
        return assignmentResponses(List.of(saved), Map.of(sow.getId(), sow)).get(0);
    }

    @Override
    public com.rit.performance.dto.response.SowMilestonePositionResponse createPosition(
            Long sowId,
            Long milestoneId,
            com.rit.performance.dto.request.SowMilestonePositionRequest request) {
        Sow sow = findSow(sowId);
        SowMilestone milestone = milestoneRepository.findByIdAndSow_Id(milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone " + milestoneId + " not found for SOW " + sowId));
        RateCard rateCard = resolveRateCard(request.getRateCardId());
        LookupValue designation = resolvePlannedDesignation(
                request.getPositionId(), rateCard);
        validateDateRange(request.getStartDate(), request.getEndDate(), "Milestone position");

        SowMilestonePosition milestonePosition = SowMilestonePosition.builder()
                .sow(sow)
                .milestone(milestone)
                .position(designation)
                .rateCard(rateCard)
                .positionName(resolvePositionName(request.getPositionName(), designation))
                .seniority(trimToNull(request.getSeniority()))
                .positionType(normalizePositionType(request.getPositionType()))
                .locationType(normalizeLocationType(request.getLocationType()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .hours(trimToNull(request.getHours()))
                .amount(request.getAmount())
                .build();
        milestone.getPositions().add(milestonePosition);
        milestone.setAmount(milestone.getPositions().stream()
                .map(SowMilestonePosition::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        milestoneRepository.saveAndFlush(milestone);

        return com.rit.performance.dto.response.SowMilestonePositionResponse.builder()
                .milestonePositionId(milestonePosition.getId())
                .positionId(designation.getId())
                .positionName(milestonePosition.getPositionName())
                .seniority(milestonePosition.getSeniority())
                .rateCardId(rateCard == null ? null : rateCard.getId())
                .hourlyRate(rateCard == null ? null : rateCard.getHourlyRate())
                .currency(rateCard == null ? null : rateCard.getCurrency())
                .positionType(milestonePosition.getPositionType())
                .locationType(milestonePosition.getLocationType())
                .startDate(milestonePosition.getStartDate())
                .endDate(milestonePosition.getEndDate())
                .hours(milestonePosition.getHours())
                .amount(milestonePosition.getAmount())
                .assignments(List.of())
                .build();
    }

    @Override
    public SowAssignmentResponse unassignFromSow(
            Long sowId,
            Long assignmentId,
            com.rit.performance.dto.request.SowAssignmentUnassignRequest request) {
        Sow sow = findSow(sowId);
        EmployeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee assignment not found: " + assignmentId));
        if (!Objects.equals(assignment.getSowId(), sowId)) {
            throw new InvalidOperationException("Assignment " + assignmentId
                    + " does not belong to SOW " + sowId);
        }
        if (!"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            throw new InvalidOperationException(
                    "Only an ACTIVE assignment can be unassigned");
        }
        if (request.getAssignmentEndDate().isBefore(assignment.getEffectiveFrom())) {
            throw new InvalidOperationException(
                    "assignmentEndDate cannot be before assignmentStartDate");
        }

        assignment.setEffectiveTo(request.getAssignmentEndDate());
        assignment.setStatus("COMPLETED");
        assignment.setIsPrimaryAssignment(false);
        assignment.setUpdatedBy(request.getUpdatedBy());
        EmployeeAssignment saved = assignmentRepository.save(assignment);
        return assignmentResponses(List.of(saved), Map.of(sowId, sow)).get(0);
    }

    private List<SowAssignmentResponse> assignmentResponses(
            List<EmployeeAssignment> assignments, Map<Long, Sow> sows) {
        Set<Long> employeeIds = assignments.stream()
                .flatMap(assignment -> java.util.stream.Stream.of(
                        assignment.getEmployeeId(), assignment.getLeadId(), assignment.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Employee> employees = employeeIds.isEmpty() ? Map.of()
                : employeeRepository.findByIdIn(List.copyOf(employeeIds)).stream()
                        .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Set<Long> designationIds = assignments.stream()
                .map(EmployeeAssignment::getDesignationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, LookupValue> designations = designationIds.isEmpty() ? Map.of()
                : lookupValueRepository.findAllById(designationIds).stream()
                        .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, SowMilestone> milestones = sows.values().stream()
                .flatMap(sow -> sow.getMilestones().stream())
                .filter(milestone -> milestone.getId() != null)
                .collect(Collectors.toMap(SowMilestone::getId, Function.identity()));

        return assignments.stream()
                .map(assignment -> {
                    Sow sow = sows.get(assignment.getSowId());
                    Employee employee = employees.get(assignment.getEmployeeId());
                    Employee lead = employees.get(assignment.getLeadId());
                    Employee manager = employees.get(assignment.getManagerId());
                    LookupValue designation = designations.get(assignment.getDesignationId());
                    SowMilestone milestone = milestones.get(assignment.getMilestoneId());
                    return SowAssignmentResponse.builder()
                            .assignmentId(assignment.getId())
                            .employeeId(assignment.getEmployeeId())
                            .employeeNumber(employee == null ? null : employee.getRitId())
                            .employeeName(employee == null ? null : employeeName(employee))
                            .email(employee == null ? null : employee.getEmail())
                            .sowId(sow.getId()).sowCode(sow.getSowCode()).sowName(sow.getSowName())
                            .milestoneId(assignment.getMilestoneId())
                            .milestoneName(assignment.getMilestoneId() == null
                                    ? "All milestones"
                                    : milestone == null ? null : milestone.getMilestoneName())
                            .designationId(assignment.getDesignationId())
                            .designationName(designation == null ? null : designation.getName())
                            .positionType(assignment.getPositionType())
                            .leadId(assignment.getLeadId())
                            .leadName(lead == null ? null : employeeName(lead))
                            .managerId(assignment.getManagerId())
                            .managerName(manager == null ? null : employeeName(manager))
                            .allocationPercentage(assignment.getAllocationPercentage())
                            .isPrimaryAssignment(assignment.getIsPrimaryAssignment())
                            .assignmentStartDate(assignment.getEffectiveFrom())
                            .assignmentEndDate(assignment.getEffectiveTo())
                            .assignmentStatus(assignment.getStatus())
                            .build();
                })
                .toList();
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
        createDraftInvoicesWhenEligible(sow, milestoneSync.retained());

        for (SowMilestone obsolete : milestoneSync.obsolete()) {
            if (featureRepository.existsByMilestone_Id(obsolete.getId())) {
                throw new InvalidOperationException(
                        "Milestone " + obsolete.getId()
                                + " cannot be removed because one or more features use it");
            }
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
                milestone = findExistingMilestoneWithoutId(existing.values(), retained, request)
                        .orElseGet(() -> {
                            SowMilestone created = new SowMilestone();
                            sow.addMilestone(created);
                            return created;
                        });
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

    private Optional<SowMilestone> findExistingMilestoneWithoutId(
            Collection<SowMilestone> existing,
            Set<SowMilestone> retained,
            SowMilestoneRequest request
    ) {
        String requestedName = request.getMilestoneName().trim();
        List<SowMilestone> nameMatches = existing.stream()
                .filter(milestone -> !retained.contains(milestone))
                .filter(milestone -> milestone.getMilestoneName() != null
                        && milestone.getMilestoneName().trim().equalsIgnoreCase(requestedName))
                .toList();
        if (nameMatches.size() == 1) return Optional.of(nameMatches.get(0));
        return nameMatches.stream()
                .filter(milestone -> Objects.equals(milestone.getStartDate(), request.getStartDate()))
                .filter(milestone -> Objects.equals(milestone.getEndDate(), request.getEndDate()))
                .findFirst();
    }

    private void createDraftInvoicesWhenEligible(Sow sow, Collection<SowMilestone> milestones) {
        String status = sow.getStatus() == null ? "" : sow.getStatus().trim().toUpperCase(Locale.ROOT);
        if (Set.of("ACTIVE", "START", "STARTED").contains(status)) {
            sowInvoiceService.createDraftInvoices(sow, milestones);
        }
    }

    private void applySowFields(Sow sow, SowRequest request) {
        sow.setSowName(request.getSowName().trim());
        sow.setSowType(request.getSowType().trim());
        sow.setEngagementType(request.getEngagementType().trim());
        sow.setBusinessUnit(lookupValueRepository.findById(request.getBusinessUnitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business unit not found: " + request.getBusinessUnitId())));
        sow.setSubmittedDate(request.getSubmittedDate());
        sow.setCsxProjectId(trimToNull(request.getCsxProjectId()));
        validateCsxEmployee(request.getProjectOwnerEmployeeId(), "Project owner");
        validateCsxEmployee(request.getCsxContactEmployeeId(), "CSX contact");
        validateCsxEmployee(request.getCsxEscalationEmployeeId(), "CSX escalation contact");
        sow.setProjectOwnerEmployeeId(request.getProjectOwnerEmployeeId());
        sow.setCsxContactEmployeeId(request.getCsxContactEmployeeId());
        sow.setCsxEscalationEmployeeId(request.getCsxEscalationEmployeeId());
        sow.setRitContactEmployee(findRitEmployee(request.getRitContactEmployeeId()));
        sow.setStartDate(request.getStartDate());
        sow.setEndDate(request.getEndDate());
        sow.setStatus(normalizeStatus(request.getStatus(), "DRAFT"));
        sow.setRemarks(normalizeDescription(request.getRemarks()));
        String signedStatus = request.getSignedStatus() == null
                || request.getSignedStatus().isBlank()
                ? "UNSIGNED"
                : request.getSignedStatus().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SIGNED", "UNSIGNED").contains(signedStatus)) {
            throw new InvalidOperationException(
                    "signedStatus must be SIGNED or UNSIGNED");
        }
        if ("SIGNED".equals(signedStatus) && request.getSignedDate() == null) {
            throw new InvalidOperationException(
                    "signedDate is required when signedStatus is SIGNED");
        }
        sow.setSignedStatus(signedStatus);
        sow.setSignedDate("SIGNED".equals(signedStatus) ? request.getSignedDate() : null);
        if (request.getDocumentList() != null) {
            synchronizeDocuments(sow, request.getDocumentList());
        }
    }

    private void applyMilestoneFields(SowMilestone milestone, SowMilestoneRequest request) {
        milestone.setMilestoneName(request.getMilestoneName().trim());
        milestone.setDescription(normalizeDescription(request.getDescription()));
        milestone.setEstimatedHours(request.getEstimatedHours());
        milestone.setStartDate(request.getStartDate());
        milestone.setEndDate(request.getEndDate());
        milestone.setInvoiceDate(request.getInvoiceDate());
        milestone.setAmount(request.getAmount());
        milestone.setStatus(normalizeStatus(request.getStatus(), "NOT_STARTED"));
        synchronizeMilestonePositions(milestone, request);
    }

    private void synchronizeMilestonePositions(SowMilestone milestone, SowMilestoneRequest request) {
        if (request.getPositions() == null) return;
        List<SowMilestonePosition> existing = new ArrayList<>(milestone.getPositions());
        Map<Long, SowMilestonePosition> existingById = existing.stream()
                .filter(position -> position.getId() != null)
                .collect(Collectors.toMap(SowMilestonePosition::getId, Function.identity()));
        Set<SowMilestonePosition> retained = new LinkedHashSet<>();

        for (int index = 0; index < request.getPositions().size(); index++) {
            var positionRequest = request.getPositions().get(index);
            SowMilestonePosition milestonePosition;
            if (positionRequest.getMilestonePositionId() != null) {
                milestonePosition = existingById.get(positionRequest.getMilestonePositionId());
                if (milestonePosition == null) {
                    throw new InvalidOperationException("Milestone position "
                            + positionRequest.getMilestonePositionId()
                            + " does not belong to milestone " + milestone.getId());
                }
            } else if (index < existing.size() && !retained.contains(existing.get(index))) {
                // Backward-compatible fallback for clients that have not started sending IDs yet.
                milestonePosition = existing.get(index);
            } else {
                milestonePosition = new SowMilestonePosition();
                milestone.addPosition(milestonePosition);
            }
            if (!retained.add(milestonePosition)) {
                throw new InvalidOperationException("Duplicate milestonePositionId in request: "
                        + positionRequest.getMilestonePositionId());
            }
            RateCard rateCard = resolveRateCard(positionRequest.getRateCardId());
            LookupValue designation = resolvePlannedDesignation(
                    positionRequest.getPositionId(), rateCard);
            String positionName = resolvePositionName(
                    positionRequest.getPositionName(), designation);
            validateDateRange(positionRequest.getStartDate(), positionRequest.getEndDate(),
                    "Milestone position");
            milestonePosition.setPosition(designation);
            milestonePosition.setRateCard(rateCard);
            milestonePosition.setPositionName(positionName);
            milestonePosition.setSeniority(trimToNull(positionRequest.getSeniority()));
            milestonePosition.setPositionType(normalizePositionType(
                    positionRequest.getPositionType()));
            milestonePosition.setLocationType(normalizeLocationType(
                    positionRequest.getLocationType()));
            milestonePosition.setStartDate(positionRequest.getStartDate());
            milestonePosition.setEndDate(positionRequest.getEndDate());
            milestonePosition.setHours(trimToNull(positionRequest.getHours()));
            milestonePosition.setAmount(positionRequest.getAmount());
        }

        List<SowMilestonePosition> removed = existing.stream()
                .filter(position -> !retained.contains(position)).toList();
        for (SowMilestonePosition position : removed) {
            if (positionAssignmentRepository.existsByMilestonePosition_Id(position.getId())) {
                throw new InvalidOperationException("Milestone position " + position.getId()
                        + " has assignment history and cannot be removed; unassign it instead");
            }
            milestone.getPositions().remove(position);
        }
    }

    private RateCard resolveRateCard(Long rateCardId) {
        if (rateCardId == null) return null;
        RateCard rateCard = rateCardRepository.findByIdWithDetails(rateCardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rate card not found: " + rateCardId));
        if (!"ACTIVE".equalsIgnoreCase(rateCard.getStatus())) {
            throw new InvalidOperationException("Rate card is not active: " + rateCardId);
        }
        return rateCard;
    }

    private LookupValue resolvePlannedDesignation(Long designationId, RateCard rateCard) {
        Long resolvedDesignationId = rateCard == null
                ? designationId : rateCard.getPositionTitleId();
        if (resolvedDesignationId == null) {
            throw new InvalidOperationException(
                    "rateCardId or designationId is required");
        }
        if (rateCard != null && designationId != null
                && !Objects.equals(designationId, resolvedDesignationId)) {
            throw new InvalidOperationException(
                    "designationId does not match the selected rate card");
        }
        return requireDesignationLookup(resolvedDesignationId);
    }

    private String resolvePositionName(String requestedName, LookupValue position) {
        String normalized = trimToNull(requestedName);
        return normalized != null ? normalized : position.getName().trim();
    }

    private LookupValue requireDesignationLookup(Long positionId) {
        LookupValue position = lookupValueRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Designation lookup not found: " + positionId));
        if (position.getLookupType() == null
                || !"DESIGNATION".equalsIgnoreCase(position.getLookupType().getCode())) {
            throw new InvalidOperationException(
                    "Position id must belong to the DESIGNATION lookup type: " + positionId);
        }
        return position;
    }

    private String normalizeDescription(String description) {
        if (description == null) return null;
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
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
        SowResponse response = SowMapper.toResponse(sow, csxEmployeesFor(List.of(sow)));
        populateResourceFulfillment(List.of(response));
        return response;
    }

    private void populateResourceFulfillment(List<SowResponse> responses) {
        Set<Long> sowIds = responses.stream().map(SowResponse::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<SowMilestonePositionAssignment> assignments = sowIds.stream()
                .flatMap(sowId -> positionAssignmentRepository
                        .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(sowId, "ACTIVE")
                        .stream())
                .toList();
        Set<Long> employeeIds = assignments.stream()
                .map(SowMilestonePositionAssignment::getEmployeeAssignment)
                .map(EmployeeAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Employee> employees = employeeIds.isEmpty() ? Map.of()
                : employeeRepository.findByIdIn(List.copyOf(employeeIds)).stream()
                        .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, List<SowMilestonePositionAssignment>> assignmentsByPosition = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getMilestonePosition().getId()));

        responses.stream().flatMap(response -> response.getMilestones().stream())
                .forEach(milestone -> milestone.getPositions().forEach(position -> {
                    List<com.rit.performance.dto.response.SowResourceAssignmentResponse> assigned =
                            assignmentsByPosition.getOrDefault(
                                    position.getMilestonePositionId(), List.of()).stream()
                                    .map(assignment -> {
                                        EmployeeAssignment sowAssignment =
                                                assignment.getEmployeeAssignment();
                                        Employee employee = employees.get(
                                                sowAssignment.getEmployeeId());
                                        return com.rit.performance.dto.response
                                                .SowResourceAssignmentResponse.builder()
                                                .assignmentId(assignment.getId())
                                                .employeeId(sowAssignment.getEmployeeId())
                                                .employeeName(employee == null
                                                        ? null : employeeName(employee))
                                                .build();
                                    }).toList();
                    position.setAssignments(assigned);
                }));
    }

    private Map<Long, CsxEmployee> csxEmployeesFor(List<Sow> sows) {
        Set<Long> employeeIds = sows.stream()
                .flatMap(sow -> java.util.stream.Stream.of(
                        sow.getProjectOwnerEmployeeId(),
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

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private Employee requireEmployee(Long employeeId, String label) {
        if (employeeId == null) return null;
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        label + " employee not found: " + employeeId));
    }

    private void validateSupervisor(Long supervisorId, Long employeeId, String label) {
        if (supervisorId == null) return;
        if (supervisorId.equals(employeeId)) {
            throw new InvalidOperationException(label + " cannot be the assigned employee");
        }
        requireEmployee(supervisorId, label);
    }

    private void validateAssignmentDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidOperationException(
                    "assignmentEndDate cannot be before assignmentStartDate");
        }
    }

    private String normalizeAssignmentStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "COMPLETED").contains(normalized)) {
            throw new InvalidOperationException(
                    "assignmentStatus must be ACTIVE or COMPLETED");
        }
        return normalized;
    }

    private String normalizePositionType(String positionType) {
        String normalized = positionType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        if ("NONBILLABLE".equals(normalized)) normalized = "NON_BILLABLE";
        if (!Set.of("BILLABLE", "NON_BILLABLE").contains(normalized)) {
            throw new InvalidOperationException(
                    "positionType must be BILLABLE or NON_BILLABLE");
        }
        return normalized;
    }

    private String normalizeLocationType(String locationType) {
        if (locationType == null || locationType.isBlank()) return null;
        String normalized = locationType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        if (!Set.of("ONSITE", "OFFSHORE").contains(normalized)) {
            throw new InvalidOperationException(
                    "locationType must be ONSITE or OFFSHORE");
        }
        return normalized;
    }

    private void clearPrimaryAssignment(Long employeeId, Long currentAssignmentId, Long updatedBy) {
        List<EmployeeAssignment> activeAssignments =
                assignmentRepository.findAllByEmployeeIdAndStatusIgnoreCase(employeeId, "ACTIVE");
        List<EmployeeAssignment> changedAssignments = activeAssignments.stream()
                .filter(other -> !Objects.equals(other.getId(), currentAssignmentId))
                .filter(other -> Boolean.TRUE.equals(other.getIsPrimaryAssignment()))
                .peek(other -> {
                    other.setIsPrimaryAssignment(false);
                    other.setUpdatedBy(updatedBy);
                })
                .toList();
        if (!changedAssignments.isEmpty()) {
            assignmentRepository.saveAll(changedAssignments);
        }
    }

    private record MilestoneSync(
            Set<SowMilestone> retained,
            Set<SowMilestone> obsolete
    ) {
    }
}
