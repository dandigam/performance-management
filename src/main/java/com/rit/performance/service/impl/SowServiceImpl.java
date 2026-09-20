package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowMilestoneRequest;
import com.rit.performance.dto.request.SowMilestoneUpdateRequest;
import com.rit.performance.dto.response.SowMilestoneResponse;
import com.rit.performance.dto.request.SowDocumentRequest;
import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.request.SowAssignmentUpdateRequest;
import com.rit.performance.dto.request.SowSignatureUpdateRequest;
import com.rit.performance.dto.request.SowStatusUpdateRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.dto.response.SowSummaryResponse;
import com.rit.performance.dto.response.SowSummaryPageResponse;
import com.rit.performance.dto.response.SowPositionSummaryResponse;
import com.rit.performance.dto.response.SowPositionSummaryPageResponse;
import com.rit.performance.dto.response.SowMilestoneSummaryResponse;
import com.rit.performance.dto.response.SowMilestoneSummaryPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.rit.performance.dto.response.SowAssignmentResponse;
import com.rit.performance.dto.SowRequirementMilestonesResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.SowMapper;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowInvoiceService;
import com.rit.performance.service.SowResourceRequirementService;
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
    private static final String SOW_STATUS_LOOKUP = "SOW_STATUS";
    private static final Map<String, Set<String>> VALID_SOW_STATUS_TRANSITIONS = Map.of(
            "DRAFT", Set.of("WAITING_FOR_APPROVAL"),
            "WAITING_FOR_APPROVAL", Set.of("APPROVED", "ACTIVE"),
            "APPROVED", Set.of("ACTIVE", "CANCELLED"),
            "ACTIVE", Set.of("ON_HOLD", "COMPLETED", "CANCELLED"),
            "ON_HOLD", Set.of("ACTIVE", "CANCELLED"));
    private final SowRepository sowRepository;
    private final SowMilestonePositionRepository positionRepository;
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
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SowResourceRequirementService resourceRequirementService;

    @Override
    public SowMilestoneResponse updateMilestone(Long sowId, Long milestoneId, SowMilestoneUpdateRequest request) {
        SowMilestone milestone = milestoneRepository.findByIdAndSow_Id(milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found for SOW: " + milestoneId));
        validateDateRange(request.getStartDate(), request.getEndDate(), "Milestone");
        milestone.setMilestoneName(request.getMilestoneName().trim());
        milestone.setDescription(normalizeDescription(request.getDescription()));
        milestone.setDeliverables(normalizeDescription(request.getDeliverables()));
        milestone.setStartDate(request.getStartDate());
        milestone.setEndDate(request.getEndDate());
        milestone.setInvoiceDate(request.getInvoiceDate());
        BigDecimal amount = request.getInvoiceAmount();
        if (amount == null) {
            amount = BigDecimal.ZERO;
            for (SowMilestonePosition position : milestone.getPositions()) {
                if (position.getAmount() != null) amount = amount.add(position.getAmount());
            }
        }
        milestone.setAmount(amount);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            milestone.setStatus(normalizeMilestoneStatus(request.getStatus()));
        }
        return SowMapper.toMilestoneResponse(milestoneRepository.saveAndFlush(milestone));
    }

    @Override
    @Transactional(readOnly = true)
    public SowPositionSummaryPageResponse getPositionSummaries(Long sowId, Long milestoneId, int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidOperationException("page must be at least 0 and size must be at least 1");
        }
        milestoneRepository.findByIdAndSow_Id(milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found for SOW: " + milestoneId));
        Page<SowMilestonePosition> positions = positionRepository.findBySow_IdAndMilestone_Id(
                sowId, milestoneId, PageRequest.of(page, size, Sort.by("id")));
        List<SowPositionSummaryResponse> content = new ArrayList<>();
        for (SowMilestonePosition position : positions) {
            SowMilestonePositionAssignment activeAssignment = null;
            // The repository orders newest first, making selection deterministic if multiple are active.
            for (SowMilestonePositionAssignment assignment : positionAssignmentRepository
                    .findByMilestonePosition_IdOrderByAssignmentStartDateDescIdDesc(position.getId())) {
                if ("ASSIGNED".equalsIgnoreCase(assignment.getStatus())) {
                    activeAssignment = assignment;
                    break;
                }
            }
            Long employeeId = activeAssignment == null ? null
                    : activeAssignment.getEmployeeAssignment().getEmployeeId();
            Employee employee = employeeId == null ? null : employeeRepository.findById(employeeId).orElse(null);
            BigDecimal estimatedHours = null;
            if (position.getHours() != null && !position.getHours().isBlank()) {
                try {
                    estimatedHours = new BigDecimal(position.getHours().trim());
                } catch (NumberFormatException ignored) {
                    // Legacy free-text hours cannot be represented as a numeric estimate.
                }
            }
            content.add(new SowPositionSummaryResponse(position.getId(), position.getPositionName(),
                    position.getLocationType(), estimatedHours, position.getStartDate(), position.getEndDate(),
                    activeAssignment == null ? position.getStatus() : "ASSIGNED", position.getPositionType(),
                    activeAssignment == null ? null : activeAssignment.getId(), employeeId,
                    employee == null ? null : employeeName(employee)));
        }
        return new SowPositionSummaryPageResponse(content, positions.getNumber(), positions.getSize(),
                positions.getTotalElements(), positions.getTotalPages(), positions.isFirst(), positions.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public SowMilestoneSummaryPageResponse getMilestoneSummaries(Long sowId, int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidOperationException("page must be at least 0 and size must be at least 1");
        }
        if (!sowRepository.existsById(sowId)) {
            throw new ResourceNotFoundException("SOW not found: " + sowId);
        }
        Page<SowMilestone> milestones = milestoneRepository.findBySow_Id(
                sowId, PageRequest.of(page, size, Sort.by("id")));
        List<SowMilestoneSummaryResponse> content = new ArrayList<>();
        for (SowMilestone milestone : milestones) {
            Set<Long> assignedPositionIds = new HashSet<>();
            for (SowMilestonePositionAssignment assignment : positionAssignmentRepository
                    .findByMilestonePosition_Milestone_IdAndStatusIgnoreCase(milestone.getId(), "ASSIGNED")) {
                assignedPositionIds.add(assignment.getMilestonePosition().getId());
            }
            int openCount = 0;
            for (SowMilestonePosition position : milestone.getPositions()) {
                if ("OPEN".equalsIgnoreCase(position.getStatus())
                        && !assignedPositionIds.contains(position.getId())) {
                    openCount++;
                }
            }
            content.add(new SowMilestoneSummaryResponse(milestone.getId(), milestone.getMilestoneName(),
                    milestone.getStartDate(), milestone.getEndDate(), milestone.getPositions().size(), openCount));
        }
        return new SowMilestoneSummaryPageResponse(content, milestones.getNumber(), milestones.getSize(),
                milestones.getTotalElements(), milestones.getTotalPages(), milestones.isFirst(), milestones.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public SowSummaryPageResponse getSummaries(int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidOperationException("page must be at least 0 and size must be at least 1");
        }
        Page<Sow> sows = sowRepository.findSummaryPage(
                PageRequest.of(page, size, Sort.by("id")));
        Map<Long, CsxEmployee> csxContacts = csxEmployeesFor(sows.getContent());
        List<SowSummaryResponse> content = new ArrayList<>();
        for (Sow sow : sows) {
            List<SowMilestonePosition> positions = positionRepository.findBySowId(sow.getId());
            Set<Long> assignedPositionIds = new HashSet<>();
            for (SowMilestonePositionAssignment assignment : positionAssignmentRepository
                    .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(sow.getId(), "ASSIGNED")) {
                assignedPositionIds.add(assignment.getMilestonePosition().getId());
            }
            int openCount = 0;
            for (SowMilestonePosition position : positions) {
                if ("OPEN".equalsIgnoreCase(position.getStatus())
                        && !assignedPositionIds.contains(position.getId())) {
                    openCount++;
                }
            }
            LookupValue businessUnit = sow.getBusinessUnit();
            CsxEmployee poc = sow.getCsxContactEmployeeId() == null ? null
                    : csxContacts.get(sow.getCsxContactEmployeeId());
            String pocName = poc == null ? null
                    : (Objects.toString(poc.getFirstName(), "").trim() + " "
                    + Objects.toString(poc.getLastName(), "").trim()).trim();
            content.add(SowSummaryResponse.builder()
                    .sowId(sow.getId()).sowName(sow.getSowName())
                    .businessUnitId(businessUnit == null ? null : businessUnit.getId())
                    .businessUnitName(businessUnit == null ? null : businessUnit.getName())
                    .pocEmployeeId(poc == null ? null : poc.getId()).pocEmployeeName(pocName)
                    .startDate(sow.getStartDate()).endDate(sow.getEndDate())
                    .status(sow.getStatus() == null ? null : sow.getStatus().getCode())
                    .totalPositionCount(positions.size()).openPositionCount(openCount).build());
        }
        return new SowSummaryPageResponse(content, sows.getNumber(), sows.getSize(),
                sows.getTotalElements(), sows.getTotalPages(), sows.isFirst(), sows.isLast());
    }

    @Override
    public SowResponse create(SowRequest request) {
        validateRequest(request);

        Sow sow = new Sow();
        applySowFields(sow, request, true);
        sowRepository.saveAndFlush(sow);

        MilestoneSync milestoneSync = synchronizeMilestones(sow, request.getMilestones());
        milestoneRepository.saveAll(milestoneSync.retained());
        milestoneRepository.flush();
        createDraftInvoicesWhenEligible(sow, milestoneSync.retained());
        Sow saved = sowRepository.saveAndFlush(sow);
        resourceRequirementService.onPositionCreatedOrUpdated(saved.getId());
        return toResponse(saved);
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
                        || (sow.getStatus() != null
                        && normalizedStatus.equalsIgnoreCase(sow.getStatus().getCode())))
                .filter(sow -> designationId == null || sow.getMilestones().stream()
                        .flatMap(milestone -> milestone.getPositions().stream())
                        .anyMatch(position -> position.getPosition() != null
                                && Objects.equals(position.getPosition().getId(), designationId)))
                .toList();
        Map<Long, CsxEmployee> csxEmployees = csxEmployeesFor(sows);
        Map<Long, String> auditorNames = auditorNamesFor(sows);
        List<SowResponse> responses = sows.stream()
                .sorted(Comparator.comparing(
                        Sow::getUpdatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(sow -> SowMapper.toResponse(sow, csxEmployees, auditorNames))
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
                .findByStatusIgnoreCaseOrderByEffectiveFromDesc("ACTIVE").stream()
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
                .findBySowIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
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
        if (request.getDesignationId() != null || request.getPositionType() != null
                || request.getMilestoneId() != null) {
            throw new InvalidOperationException(
                    "Update milestone, designation and position type through the milestone position assignment");
        }
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
        }

        assignment.setLeadId(request.getLeadId());
        assignment.setManagerId(request.getManagerId());


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
        LookupValue skill = resolvePlannedSkill(request.getSkillId(), rateCard);
        LookupValue seniority = resolveSeniority(request.getSeniorityId());
        validateDateRange(request.getStartDate(), request.getEndDate(), "Milestone position");

        SowMilestonePosition milestonePosition = SowMilestonePosition.builder()
                .sow(sow)
                .milestone(milestone)
                .position(designation)
                .skill(skill)
                .rateCard(rateCard)
                .positionName(resolvePositionName(request.getPositionName(), designation))
                .seniority(seniority)
                .positionType(normalizePositionType(request.getPositionType()))
                .locationType(normalizeLocationType(request.getLocationType()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .hours(trimToNull(request.getHours()))
                .amount(request.getAmount())
                .build();
        applyPositionRate(milestonePosition, rateCard, request);
        milestone.getPositions().add(milestonePosition);
        milestone.setAmount(milestone.getPositions().stream()
                .map(SowMilestonePosition::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        milestoneRepository.saveAndFlush(milestone);
        resourceRequirementService.onPositionCreatedOrUpdated(sowId);

        return com.rit.performance.dto.response.SowMilestonePositionResponse.builder()
                .milestonePositionId(milestonePosition.getId())
                .positionId(designation.getId())
                .positionName(milestonePosition.getPositionName())
                .skillId(skill.getId())
                .skillName(skill.getName())
                .seniorityId(seniority.getId())
                .seniority(seniority.getName())
                .rateCardId(rateCard == null ? null : rateCard.getId())
                .hourlyRate(milestonePosition.getHourlyRate())
                .rateOverrideReason(milestonePosition.getRateOverrideReason())
                .rateUpdatedBy(milestonePosition.getRateUpdatedBy())
                .rateUpdatedDate(milestonePosition.getRateUpdatedDate())
                .currency(rateCard == null ? null : rateCard.getCurrency())
                .positionType(milestonePosition.getPositionType())
                .status(milestonePosition.getStatus() == null
                        ? "OPEN" : milestonePosition.getStatus())
                .locationType(milestonePosition.getLocationType())
                .startDate(milestonePosition.getStartDate())
                .endDate(milestonePosition.getEndDate())
                .hours(milestonePosition.getHours())
                .amount(milestonePosition.getAmount())
                .assignments(List.of())
                .build();
    }

    @Override
    public void deletePosition(Long sowId, Long milestoneId, Long positionId) {
        SowMilestone milestone = milestoneRepository.findByIdAndSow_Id(milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone " + milestoneId + " not found for SOW " + sowId));
        SowMilestonePosition position = milestone.getPositions().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), positionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone position " + positionId + " not found for milestone "
                                + milestoneId + " and SOW " + sowId));
        if (positionAssignmentRepository.existsByMilestonePosition_Id(positionId)) {
            throw new InvalidOperationException("POSITION_HAS_RESOURCE",
                    "Position can't be removed. Unassign the resource first.");
        }

        milestone.getPositions().remove(position);
        milestone.setAmount(milestone.getPositions().stream()
                .map(SowMilestonePosition::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        milestoneRepository.saveAndFlush(milestone);

        // Reconcile the derived requirement rows after the position has been removed.
        resourceRequirementService.onPositionRemoved(sowId);
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
        assignment.setEffectiveTo(request.getAssignmentEndDate());
        assignment.setStatus("COMPLETED");

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
        Map<Long, List<SowMilestonePositionAssignment>> details = assignments.isEmpty() ? Map.of()
                : positionAssignmentRepository.findByEmployeeAssignment_IdIn(
                        assignments.stream().map(EmployeeAssignment::getId).toList()).stream()
                        .collect(Collectors.groupingBy(item -> item.getEmployeeAssignment().getId()));
        Set<Long> designationIds = details.values().stream().flatMap(List::stream)
                .map(SowMilestonePositionAssignment::getMilestonePosition)
                .filter(Objects::nonNull).map(SowMilestonePosition::getPosition)
                .filter(Objects::nonNull).map(LookupValue::getId).collect(Collectors.toSet());
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
                    var summary = com.rit.performance.service.EmployeeAssignmentSummary.from(sow,
                            details.getOrDefault(assignment.getId(), List.of()));
                    Employee employee = assignment.getEmployeeId() == null
                            ? null : employees.get(assignment.getEmployeeId());
                    Employee lead = assignment.getLeadId() == null
                            ? null : employees.get(assignment.getLeadId());
                    Employee manager = assignment.getManagerId() == null
                            ? null : employees.get(assignment.getManagerId());
                    LookupValue designation = summary.getDesignationId() == null
                            ? null : designations.get(summary.getDesignationId());
                    SowMilestone milestone = summary.getMilestoneId() == null
                            ? null : milestones.get(summary.getMilestoneId());
                    return SowAssignmentResponse.builder()
                            .assignmentId(assignment.getId())
                            .employeeId(assignment.getEmployeeId())
                            .employeeNumber(employee == null ? null : employee.getRitId())
                            .employeeName(employee == null ? null : employeeName(employee))
                            .email(employee == null ? null : employee.getEmail())
                            .sowId(sow.getId()).sowName(sow.getSowName())
                            .milestoneId(summary.getMilestoneId())
                            .milestoneName(summary.getMilestoneId() == null
                                    ? "All milestones"
                                    : milestone == null ? null : milestone.getMilestoneName())
                            .designationId(summary.getDesignationId())
                            .designationName(designation == null ? null : designation.getName())
                            .positionType(summary.getPositionType())
                            .leadId(assignment.getLeadId())
                            .leadName(lead == null ? null : employeeName(lead))
                            .managerId(assignment.getManagerId())
                            .managerName(manager == null ? null : employeeName(manager))
                            .isPrimaryAssignment(null)
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
        applySowFields(sow, request, false);

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
        Sow saved = sowRepository.saveAndFlush(sow);
        resourceRequirementService.onPositionCreatedOrUpdated(saved.getId());
        return toResponse(saved);
    }

    @Override
    public SowResponse updateStatus(Long sowId, SowStatusUpdateRequest request) {
        Sow sow = findSow(sowId);
        LookupValue newStatus = resolveSowStatus(request.getStatus());
        String currentCode = sow.getStatus().getCode().toUpperCase(Locale.ROOT);
        String newCode = newStatus.getCode().toUpperCase(Locale.ROOT);

        if (!VALID_SOW_STATUS_TRANSITIONS
                .getOrDefault(currentCode, Set.of()).contains(newCode)) {
            throw new InvalidOperationException(
                    "Invalid SOW status transition: " + currentCode + " -> " + newCode);
        }

        LocalDate effectiveDate = request.getStatusEffectiveDate();
        if (effectiveDate.isAfter(LocalDate.now())) {
            throw new InvalidOperationException("statusEffectiveDate cannot be in the future");
        }
        sow.setStatus(newStatus);
        sow.setStatusEffectiveDate(effectiveDate);
        return toResponse(sowRepository.save(sow));
    }

    @Override
    public SowResponse updateSignature(Long sowId, SowSignatureUpdateRequest request) {
        Sow sow = findSow(sowId);
        String signedStatus = request.getSignedStatus().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SIGNED", "UNSIGNED").contains(signedStatus)) {
            throw new InvalidOperationException(
                    "signedStatus must be SIGNED or UNSIGNED");
        }
        if ("SIGNED".equals(signedStatus) && request.getSignedDate() == null) {
            throw new InvalidOperationException(
                    "signedDate is required when signedStatus is SIGNED");
        }
        if (request.getSignedDate() != null && request.getSignedDate().isAfter(LocalDate.now())) {
            throw new InvalidOperationException("signedDate cannot be in the future");
        }

        sow.setSignedStatus(signedStatus);
        sow.setSignedDate("SIGNED".equals(signedStatus) ? request.getSignedDate() : null);
        return toResponse(sowRepository.save(sow));
    }

    @Override
    public void delete(Long id) {
        Sow sow = findSow(id);
        resourceRequirementService.clear(id);
        sowRepository.delete(sow);
    }

    @Override
    @Transactional(readOnly = true)
    public SowRequirementMilestonesResponse getMilestonesByPosition(
            Long sowId, Long positionId, Long skillId, Long seniorityId, String location) {
        if (!sowRepository.existsById(sowId)) {
            throw new ResourceNotFoundException("SOW not found: " + sowId);
        }
        requireDesignationLookup(positionId);
        resolvePlannedSkill(skillId, null);
        resolveSeniority(seniorityId);
        return resourceRequirementService.getMilestonesByPosition(
                sowId, positionId, skillId, seniorityId, location);
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
        String status = sow.getStatus() == null ? "" : sow.getStatus().getCode();
        if ("ACTIVE".equals(status)) {
            sowInvoiceService.createDraftInvoices(sow, milestones);
        }
    }

    private void applySowFields(Sow sow, SowRequest request, boolean applyWorkflowFields) {
        sow.setSowName(request.getSowName().trim());
        sow.setYear(request.getYear());
        sow.setClient(clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found: " + request.getClientId())));
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
        sow.setRitContactEmployee(findRitEmployee(
                request.getRitContactEmployeeId(), "RIT contact"));
        sow.setRitEscalationEmployee(findRitEmployee(
                request.getRitEscalationEmployeeId(), "RIT escalation person"));
        sow.setStartDate(request.getStartDate());
        sow.setEndDate(request.getEndDate());
        if (applyWorkflowFields) {
            LookupValue resolvedStatus = resolveSowStatus(request.getStatus());
            sow.setStatus(resolvedStatus);
            sow.setStatusEffectiveDate(request.getStatusEffectiveDate() == null
                    ? LocalDate.now() : request.getStatusEffectiveDate());
        }
        sow.setRemarks(normalizeDescription(request.getRemarks()));
        if (applyWorkflowFields) {
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
        }
        if (request.getDocumentList() != null) {
            synchronizeDocuments(sow, request.getDocumentList());
        }
    }

    private void applyMilestoneFields(SowMilestone milestone, SowMilestoneRequest request) {
        milestone.setMilestoneName(request.getMilestoneName().trim());
        milestone.setDescription(normalizeDescription(request.getDescription()));
        milestone.setDeliverables(normalizeDescription(request.getDeliverables()));
        milestone.setEstimatedHours(request.getEstimatedHours());
        milestone.setStartDate(request.getStartDate());
        milestone.setEndDate(request.getEndDate());
        milestone.setInvoiceDate(request.getInvoiceDate());
        milestone.setAmount(request.getAmount());
        milestone.setStatus(normalizeMilestoneStatus(request.getStatus()));
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
            boolean newPosition = false;
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
                newPosition = true;
            }
            if (!retained.add(milestonePosition)) {
                throw new InvalidOperationException("Duplicate milestonePositionId in request: "
                        + positionRequest.getMilestonePositionId());
            }
            RateCard rateCard = resolveRateCard(positionRequest.getRateCardId());
            LookupValue designation = resolvePlannedDesignation(
                    positionRequest.getPositionId(), rateCard);
            LookupValue skill = resolvePlannedSkill(positionRequest.getSkillId(), rateCard);
            LookupValue seniority = resolveSeniority(positionRequest.getSeniorityId());
            String positionName = resolvePositionName(
                    positionRequest.getPositionName(), designation);
            validateDateRange(positionRequest.getStartDate(), positionRequest.getEndDate(),
                    "Milestone position");
            milestonePosition.setPosition(designation);
            milestonePosition.setSkill(skill);
            milestonePosition.setRateCard(rateCard);
            applyPositionRate(milestonePosition, rateCard, positionRequest);
            milestonePosition.setPositionName(positionName);
            milestonePosition.setSeniority(seniority);
            milestonePosition.setPositionType(normalizePositionType(
                    positionRequest.getPositionType()));
            milestonePosition.setLocationType(normalizeLocationType(
                    positionRequest.getLocationType()));
            milestonePosition.setStartDate(positionRequest.getStartDate());
            milestonePosition.setEndDate(positionRequest.getEndDate());
            milestonePosition.setHours(trimToNull(positionRequest.getHours()));
            milestonePosition.setAmount(positionRequest.getAmount());
            if (newPosition) {
                // Attach only after mandatory fields are populated. Repository lookups above can
                // trigger an automatic flush of managed entities.
                milestone.addPosition(milestonePosition);
            }
        }

        List<SowMilestonePosition> removed = existing.stream()
                .filter(position -> !retained.contains(position)).toList();
        for (SowMilestonePosition position : removed) {
            if (positionAssignmentRepository.existsByMilestonePosition_Id(position.getId())) {
                throw new InvalidOperationException("POSITION_HAS_RESOURCE",
                        "Position can't be removed. Unassign the resource first.");
            }
            milestone.getPositions().remove(position);
        }
    }

    private void applyPositionRate(SowMilestonePosition position, RateCard rateCard,
            com.rit.performance.dto.request.SowMilestonePositionRequest request) {
        BigDecimal cardRate = rateCard == null ? null : rateCard.getHourlyRate();
        BigDecimal requestedRate = request.getHourlyRate() == null
                ? cardRate : request.getHourlyRate();
        String reason = trimToNull(request.getRateOverrideReason());
        boolean overrideMetadataProvided = reason != null || request.getRateUpdatedBy() != null;
        boolean overridden = requestedRate != null
                && ((cardRate != null && requestedRate.compareTo(cardRate) != 0)
                || (cardRate == null && overrideMetadataProvided));
        if (overridden) {
            if (reason == null) {
                throw new InvalidOperationException(
                        "rateOverrideReason is required when hourlyRate overrides the rate card");
            }
            if (request.getRateUpdatedBy() == null) {
                throw new InvalidOperationException(
                        "rateUpdatedBy is required when hourlyRate overrides the rate card");
            }
            position.setRateOverrideReason(reason);
            position.setRateUpdatedBy(request.getRateUpdatedBy());
            position.setRateUpdatedDate(java.time.LocalDateTime.now());
        } else {
            position.setRateOverrideReason(null);
            position.setRateUpdatedBy(null);
            position.setRateUpdatedDate(null);
        }
        position.setHourlyRate(requestedRate);
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

    private LookupValue resolvePlannedSkill(Long skillId, RateCard rateCard) {
        Long rateCardSkillId = rateCard == null || rateCard.getMainSkill() == null
                ? null : rateCard.getMainSkill().getId();
        Long resolvedSkillId = rateCardSkillId == null ? skillId : rateCardSkillId;
        if (resolvedSkillId == null) {
            throw new InvalidOperationException("rateCardId or skillId is required");
        }
        if (rateCardSkillId != null && skillId != null
                && !Objects.equals(skillId, rateCardSkillId)) {
            throw new InvalidOperationException(
                    "skillId does not match the selected rate card");
        }
        LookupValue skill = lookupValueRepository.findById(resolvedSkillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Skill lookup not found: " + resolvedSkillId));
        if (skill.getLookupType() == null
                || !"SKILL".equalsIgnoreCase(skill.getLookupType().getCode())) {
            throw new InvalidOperationException(
                    "Skill id must belong to the SKILL lookup type: " + resolvedSkillId);
        }
        return skill;
    }

    private LookupValue resolveSeniority(Long seniorityId) {
        LookupValue seniority = lookupValueRepository.findById(seniorityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seniority lookup not found: " + seniorityId));
        if (seniority.getLookupType() == null
                || !"SENIORITY".equalsIgnoreCase(seniority.getLookupType().getCode())) {
            throw new InvalidOperationException(
                    "seniorityId must belong to the SENIORITY lookup type: " + seniorityId);
        }
        return seniority;
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

    private Employee findRitEmployee(Long id, String label) {
        if (id == null) return null;
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        label + " employee not found: " + id));
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
        SowResponse response = SowMapper.toResponse(
                sow, csxEmployeesFor(List.of(sow)), auditorNamesFor(List.of(sow)));
        populateResourceFulfillment(List.of(response));
        return response;
    }

    private void populateResourceFulfillment(List<SowResponse> responses) {
        Set<Long> sowIds = responses.stream().map(SowResponse::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<SowMilestonePositionAssignment> assignments = sowIds.stream()
                .flatMap(sowId -> positionAssignmentRepository
                        .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(sowId, "ASSIGNED")
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

    private Map<Long, String> auditorNamesFor(List<Sow> sows) {
        Set<Long> userIds = sows.stream()
                .flatMap(sow -> java.util.stream.Stream.of(
                        sow.getCreatedBy(), sow.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::auditorDisplayName));
    }

    private String auditorDisplayName(User user) {
        if (user.getEmployee() == null) return user.getUsername();
        String name = employeeName(user.getEmployee());
        return name.isBlank() ? user.getUsername() : name;
    }

    private LookupValue resolveSowStatus(String value) {
        String requested = value == null || value.isBlank() ? "DRAFT" : value;
        String normalized = requested.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if ("WAITINGFORAPPROVAL".equals(normalized)) normalized = "WAITING_FOR_APPROVAL";
        if ("ONHOLD".equals(normalized)) normalized = "ON_HOLD";
        String statusCode = normalized;
        return lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        SOW_STATUS_LOOKUP, statusCode)
                .orElseThrow(() -> new InvalidOperationException(
                        "Invalid or inactive SOW status: " + statusCode));
    }

    private String normalizeMilestoneStatus(String value) {
        if (value == null || value.isBlank()) return "NOT_STARTED";
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() > 30) {
            throw new InvalidOperationException(
                    "milestone status must not exceed 30 characters");
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



    private record MilestoneSync(
            Set<SowMilestone> retained,
            Set<SowMilestone> obsolete
    ) {
    }
}
