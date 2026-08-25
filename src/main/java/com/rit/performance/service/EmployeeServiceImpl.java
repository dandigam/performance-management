package com.rit.performance.service;

import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.dto.EmployeeUpdateRequest;
import com.rit.performance.dto.EmployeeCreateRequest;
import com.rit.performance.dto.EmployeeCreateResponse;
import com.rit.performance.dto.EmployeeReviewSummaryResponse;
import com.rit.performance.dto.ReportingManagerResponse;
import com.rit.performance.dto.DirectReportsResponse;
import com.rit.performance.dto.EmployeeHierarchyMemberResponse;
import com.rit.performance.dto.EmployeeHierarchyResponse;
import com.rit.performance.dto.EmployeeInformationResponse;
import com.rit.performance.dto.EmployeeAddressRequest;
import com.rit.performance.dto.EmployeeAddressResponse;
import com.rit.performance.dto.EmployeeCompensationRequest;
import com.rit.performance.dto.EmployeeCompensationResponse;
import com.rit.performance.dto.EmployeeFinanceHistoryResponse;
import com.rit.performance.dto.EmployeeProfessionalDetailsRequest;
import com.rit.performance.dto.EmployeeProfessionalDetailsResponse;
import com.rit.performance.dto.EmployeeBankDetailsRequest;
import com.rit.performance.dto.EmployeeBankDetailsResponse;
import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.EmployeeDocumentRequest;
import com.rit.performance.dto.EmployeeAssignmentRequest;
import com.rit.performance.dto.EmployeeAssignmentResponse;
import com.rit.performance.dto.EmployeeAssignmentsResponse;
import com.rit.performance.dto.ProjectAssignmentRequest;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;
import com.rit.performance.dto.request.SowMilestonePositionAssignmentRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeAddressRepository;
import com.rit.performance.repository.EmployeeCompensationRepository;
import com.rit.performance.repository.EmployeeProfessionalProfileRepository;
import com.rit.performance.repository.BankAccountRepository;
import com.rit.performance.repository.DocumentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.repository.VendorRepository;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DEFAULT_ROLE = "EMPLOYEE";
    private static final Set<String> ALLOWED_GENDERS = Set.of(
            "MALE", "FEMALE", "NON_BINARY", "OTHER", "PREFER_NOT_TO_SAY");

    private final EmployeeRepository employeeRepository;
    private final EmployeeAddressRepository employeeAddressRepository;
    private final EmployeeCompensationRepository employeeCompensationRepository;
    private final EmployeeProfessionalProfileRepository employeeProfessionalProfileRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DocumentRepository documentRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final LookupValueRepository lookupValueRepository;
    private final SowRepository sowRepository;
    private final SowMilestoneRepository sowMilestoneRepository;
    private final UserRepository userRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final EmployeeReviewAssessmentRepository employeeReviewAssessmentRepository;
    private final EmployeeReviewRepository employeeReviewRepository;
    private final PerformanceCycleConfigRepository cycleRepository;
    private final PerformanceCycleAssessorRepository assessorConfigRepository;
    private final VendorRepository vendorRepository;
    private final AssessmentAssigneeResolver assigneeResolver;
    private final SowMilestonePositionAssignmentService positionAssignmentService;

    @Override
    @Transactional
    public EmployeeCreateResponse create(EmployeeCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(email) || userRepository.existsByUsernameIgnoreCase(email))
            throw new InvalidOperationException("Employee email or username already exists: " + email);
        String csxRacfId = normalizeIdentifier(request.getCsxRacfId());
        validateIdentifiersAvailable(null, csxRacfId, null);
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName() == null ? null : request.getLastName().trim());
        employee.setEmail(email);
        employee.setPhoneNumber(request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim());
        employee.setGender(normalizeGender(request.getGender()));
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setCsxRacfId(csxRacfId);
        employee.setEmploymentType(normalizeRequiredValue(request.getEmploymentType(), "employmentType"));
        employee.setJoiningDate(request.getJoiningDate());
        employee.setWorkMode(normalizeRequiredValue(request.getWorkMode(), "workMode"));
        employee.setVendor(resolveVendor(request.getVendorId(), employee.getEmploymentType()));
        employee.setDesignationId(resolveProfileDesignationId(request));
        employee.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus().trim().toUpperCase());
        employee.setCreatedBy(request.getCreatedBy());
        employee.setUpdatedBy(request.getCreatedBy());
        employee = employeeRepository.save(employee);
        employee.setRitId(formatRitEmployeeId(employee.getId()));
        saveAddress(employee, request.getAddressDetails());
        saveCompensation(employee, request.getCompensationDetails());
        saveProfessionalDetails(employee, request.getProfessionalDetails());
        saveBankDetails(employee, request.getBankDetails());
        synchronizeDocuments(employee, request.getDocumentList());
        EmployeeAssignment assignment = createInitialAssignment(employee.getId(), request);
        LookupValue role = requestedEmployeeRole(request.getRoleId());
        createEmployeeRole(employee, role, LocalDate.now(), request.getCreatedBy());
        User user = new User();
        user.setUsername(email);
        user.setPassword(DEFAULT_PASSWORD);
        user.setStatus("ACTIVE");
        user.setRole(role);
        user.setEmployee(employee);
        user = userRepository.save(user);

        if (assignment != null) {
            seedReviewsForNewEmployee(employee, request.getCreatedBy(), assignment);
        }

        return EmployeeCreateResponse.builder().employee(currentEmployeeResponse(employee, assignment))
                .userId(user.getId()).username(user.getUsername()).password(DEFAULT_PASSWORD)
                .roleName(role.getName()).build();
    }

    private void seedReviewsForNewEmployee(Employee employee, Long actorId, EmployeeAssignment assignment) {
        List<PerformanceCycles> cycles = cycleRepository.findAll().stream()
                .filter(cycle -> "PUBLISHED".equalsIgnoreCase(cycle.getStatus())
                        || "ACTIVE".equalsIgnoreCase(cycle.getStatus()))
                .toList();
        for (PerformanceCycles cycle : cycles) {
            if (!employeeIsEligibleForCycle(employee, cycle)) {
                continue;
            }
            EmployeeReview savedReview = employeeReviewRepository
                    .findByEmployeeIdAndPerformanceCycleId(employee.getId(), cycle.getId())
                    .orElseGet(() -> employeeReviewRepository.save(EmployeeReview.builder()
                            .performanceCycle(cycle)
                            .employee(employee)
                            .status(EmployeeReviewStatus.NOT_STARTED)
                            .progressPercentage(BigDecimal.ZERO)
                            .sowId(assignment == null ? null : assignment.getSowId())
                            .createdBy(actorId)
                            .updatedBy(actorId)
                            .build()));
            if (savedReview.getSowId() == null && assignment != null) {
                savedReview.setSowId(assignment.getSowId());
                employeeReviewRepository.save(savedReview);
            }

            List<PerformanceCycleAssessor> assessorConfigs = assessorConfigRepository
                    .findByPerformanceCycleIdOrderByDisplayOrderAsc(cycle.getId()).stream()
                    .filter(config -> Boolean.TRUE.equals(config.getActive()))
                    .filter(config -> !employeeReviewAssessmentRepository
                            .existsByEmployeeReviewIdAndAssessmentLevel(
                                    savedReview.getId(), config.getDisplayOrder()))
                    .toList();
            List<EmployeeReviewAssessment> assessments = assessorConfigs.stream()
                    .map(config -> newAssessment(savedReview, config, actorId))
                    .filter(Objects::nonNull)
                    .toList();
            if (!assessments.isEmpty()) {
                employeeReviewAssessmentRepository.saveAll(assessments);
            }
        }
    }

    private boolean employeeIsEligibleForCycle(Employee employee, PerformanceCycles cycle) {
        LookupValue applicableType = lookupValueRepository.findById(cycle.getApplicableTypeId())
                .orElseThrow(() -> new InvalidOperationException(
                        "Cycle has an invalid applicable type: " + cycle.getId()));
        String code = applicableType.getCode() == null ? "" : applicableType.getCode().toUpperCase();
        List<Long> scopeIds = cycle.getScopeValueIds() == null ? List.of() : cycle.getScopeValueIds();
        return switch (code) {
            case "ALL" -> true;
            case "DEPARTMENT" -> assignmentRepository.findByDepartmentIdInAndStatusIgnoreCase(scopeIds, "ACTIVE").stream()
                    .map(EmployeeAssignment::getEmployeeId)
                    .anyMatch(employee.getId()::equals);
            case "DESIGNATION" -> employee.getDesignationId() != null
                    && scopeIds.contains(employee.getDesignationId());
            case "EMPLOYEE" -> scopeIds.contains(employee.getId());
            default -> false;
        };
    }

    private EmployeeReviewAssessment newAssessment(EmployeeReview review, PerformanceCycleAssessor config,
            Long createdBy) {
        LookupValue role = lookupValueRepository.findById(config.getRoleId())
                .orElseThrow(() -> new InvalidOperationException(
                        "Invalid assessor role: " + config.getRoleId()));
        if (assigneeResolver.isPublishOnly(role)) return null;
        if (!assigneeResolver.isApplicable(review.getEmployee(), role)) return null;
        Employee assessor = assigneeResolver.resolveIfAvailable(review.getEmployee(), role).orElse(null);
        if (assessor == null) return null;
        return EmployeeReviewAssessment.builder().employeeReview(review)
                .assessmentLevel(config.getDisplayOrder()).assessorRole(role).assessorEmployee(assessor)
                .status(EmployeeReviewStatus.NOT_STARTED).progressPercentage(BigDecimal.ZERO)
                .createdBy(createdBy).updatedBy(createdBy).build();
    }

    private EmployeeAssignment createInitialAssignment(Long employeeId, EmployeeCreateRequest request) {
        ProjectAssignmentRequest nested = request.getProjectAssignment();
        if (nested == null) return null;
        Long designationId = nested.getDesignationId() == null
                ? request.getDesignationId() : nested.getDesignationId();
        Long sowId = nested.getSowId();
        Long departmentId = sowId == null ? nested.getDepartmentId() : departmentIdForSow(sowId);
        Long leadId = nested.getLeadId();
        Long managerId = nested.getManagerId();
        LocalDate effectiveFrom = nested.getEffectiveFrom();
        validateAssignmentValues(employeeId, departmentId, designationId, sowId, nested.getMilestoneId(),
                leadId, managerId);

        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployeeId(employeeId);
        assignment.setDepartmentId(departmentId);
        assignment.setDesignationId(designationId);
        assignment.setLeadId(leadId);
        assignment.setManagerId(managerId);
        assignment.setSowId(sowId);
        assignment.setMilestoneId(nested.getMilestoneId());
        assignment.setPositionType(normalizePositionType(nested.getPositionType()));
        assignment.setEffectiveFrom(effectiveFrom == null ? LocalDate.now() : effectiveFrom);
        assignment.setEffectiveTo(nested.getAssignmentEndDate());
        validateAssignmentDates(assignment.getEffectiveFrom(), assignment.getEffectiveTo());
        assignment.setAllocationPercentage(nested.getAllocationPercentage() == null
                ? 100 : nested.getAllocationPercentage());
        assignment.setStatus(normalizeAssignmentStatus(nested.getStatus()));
        assignment.setIsPrimaryAssignment(true);
        assignment.setCreatedBy(request.getCreatedBy());
        assignment.setUpdatedBy(request.getCreatedBy());
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public EmployeeBasicInfoResponse assign(EmployeeAssignmentRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));
        Long departmentId = departmentIdForSow(request.getSowId());
        validateAssignmentValues(employee.getId(), departmentId, null,
                request.getSowId(), null, request.getLeadId(), request.getManagerId());
        Optional<EmployeeAssignment> existingAssignment = assignmentRepository
                .findFirstBySowIdAndEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
                        request.getSowId(), employee.getId(), "ACTIVE");

        EmployeeAssignment assignment;
        boolean primary;
        if (existingAssignment.isPresent()) {
            assignment = existingAssignment.get();
            primary = Boolean.TRUE.equals(assignment.getIsPrimaryAssignment())
                    || Boolean.TRUE.equals(request.getIsPrimaryAssignment());
            if (Boolean.TRUE.equals(request.getIsPrimaryAssignment())
                    && !Boolean.TRUE.equals(assignment.getIsPrimaryAssignment())) {
                clearPrimaryAssignment(employee.getId(), request.getUpdatedBy());
            }
            if (request.getLeadId() != null) assignment.setLeadId(request.getLeadId());
            if (request.getManagerId() != null) assignment.setManagerId(request.getManagerId());
            assignment.setIsPrimaryAssignment(primary);
            assignment.setUpdatedBy(request.getUpdatedBy());
            assignment = assignmentRepository.save(assignment);
        } else {
            primary = Boolean.TRUE.equals(request.getIsPrimaryAssignment())
                    || !assignmentRepository
                            .existsByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrue(
                                    employee.getId(), "ACTIVE");
            if (primary) clearPrimaryAssignment(employee.getId(), request.getUpdatedBy());

            assignment = new EmployeeAssignment();
            assignment.setEmployeeId(employee.getId());
            assignment.setSowId(request.getSowId());
            assignment.setMilestoneId(null);
            assignment.setDepartmentId(departmentId);
            assignment.setDesignationId(null);
            assignment.setLeadId(request.getLeadId());
            assignment.setManagerId(request.getManagerId());
            assignment.setPositionType(null);
            assignment.setAllocationPercentage(100);
            LocalDate firstMilestoneStart = request.getMilestoneAssignments().stream()
                    .map(com.rit.performance.dto.EmployeeMilestoneAssignmentRequest
                            ::getAssignmentStartDate)
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            assignment.setEffectiveFrom(request.getEffectiveFrom() == null
                    ? firstMilestoneStart : request.getEffectiveFrom());
            assignment.setStatus("ACTIVE");
            assignment.setIsPrimaryAssignment(primary);
            assignment.setCreatedBy(request.getUpdatedBy());
            assignment.setUpdatedBy(request.getUpdatedBy());
            assignment = assignmentRepository.save(assignment);
            seedReviewsForNewEmployee(employee, request.getUpdatedBy(), assignment);
        }
        Long employeeAssignmentId = assignment.getId();
        List<SowMilestonePositionAssignmentResponse> milestoneAssignments = request
                .getMilestoneAssignments().stream()
                .map(item -> positionAssignmentService.create(request.getSowId(),
                        item.getMilestoneId(), item.getMilestonePositionId(),
                        SowMilestonePositionAssignmentRequest.builder()
                                .employeeAssignmentId(employeeAssignmentId)
                                .allocationPercentage(item.getAllocationPercentage() == null
                                        ? 100 : item.getAllocationPercentage())
                                .positionType(item.getPositionType())
                                .assignmentStartDate(item.getAssignmentStartDate())
                                .assignmentEndDate(item.getAssignmentEndDate())
                                .status(item.getStatus() == null || item.getStatus().isBlank()
                                        ? "ACTIVE" : item.getStatus())
                                .updatedBy(request.getUpdatedBy())
                                .build()))
                .toList();
        EmployeeBasicInfoResponse response = currentEmployeeResponse(employee, primary
                ? assignment : assignmentRepository.findActiveByEmployeeId(employee.getId())
                        .orElse(assignment));
        response.setMilestoneAssignments(milestoneAssignments);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeBasicInfoResponse> getBasicInfo() {
        List<Employee> employees = employeeRepository.findAll();
        Map<Long, Employee> employeesById = employees.stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, List<EmployeeAssignment>> assignmentsByEmployee = assignmentRepository.findAll().stream()
                .sorted(assignmentDisplayOrder())
                .collect(Collectors.groupingBy(EmployeeAssignment::getEmployeeId));
        Map<Long, EmployeeAssignment> currentAssignments = assignmentsByEmployee.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .filter(assignment -> "ACTIVE".equalsIgnoreCase(assignment.getStatus()))
                        .findFirst().map(assignment -> Map.entry(entry.getKey(), assignment)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Long, EmployeeRole> currentRoles = employeeRoleRepository.findByIsCurrentTrue().stream()
                .sorted(Comparator.comparing(EmployeeRole::getEffectiveFrom).reversed())
                .collect(Collectors.toMap(EmployeeRole::getEmployeeId, Function.identity(), (first, ignored) -> first));
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Sow> sows = sowRepository.findAll().stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        Map<Long, SowMilestone> milestones = sowMilestoneRepository.findAll().stream()
                .collect(Collectors.toMap(SowMilestone::getId, Function.identity()));

        return employees.stream()
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> toResponse(employee, currentAssignments.get(employee.getId()),
                        currentRoles.get(employee.getId()), employeesById, lookupValues, sows, null,
                        assignmentsByEmployee.getOrDefault(employee.getId(), List.of()), milestones))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeBasicInfoResponse getById(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        EmployeeAssignment assignment = assignmentRepository.findActiveByEmployeeId(employeeId)
                .orElse(null);
        return currentEmployeeResponse(employee, assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeAssignmentsResponse getAssignmentsByEmployeeId(Long employeeId) {
        EmployeeBasicInfoResponse response = getById(employeeId);
        return EmployeeAssignmentsResponse.builder()
                .employeeId(response.getEmployeeId())
                .employeeName(response.getEmployeeName())
                .assignmentList(response.getAssignmentList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeFinanceHistoryResponse> getFinanceHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }
        List<EmployeeCompensation> history = employeeCompensationRepository
                .findByEmployeeIdOrderByEffectiveDateDescIdDesc(employeeId);
        return java.util.stream.IntStream.range(0, history.size())
                .mapToObj(index -> {
                    EmployeeCompensation compensation = history.get(index);
                    LocalDate endDate = index == 0 ? null
                            : history.get(index - 1).getEffectiveDate().minusDays(1);
                    return EmployeeFinanceHistoryResponse.builder()
                        .id(compensation.getId())
                        .payType(compensation.getPayType())
                        .hourlyRate(compensation.getHourlyRate())
                        .amount(compensation.getHourlyRate())
                        .currency(compensation.getCurrency())
                        .effectiveDate(compensation.getEffectiveDate())
                        .endDate(endDate)
                        .reason(compensation.getReason())
                        .status(compensation.isCurrent() ? "CURRENT" : "HISTORICAL")
                        .current(compensation.isCurrent())
                        .createdAt(compensation.getCreatedAt())
                        .updatedAt(compensation.getUpdatedAt())
                        .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeInformationResponse> getEmployeeInformation() {
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        return employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> EmployeeInformationResponse.builder()
                        .employeeId(employee.getId())
                        .employeeName(employeeName(employee))
                        .firstName(employee.getFirstName())
                        .lastName(employee.getLastName())
                        .email(employee.getEmail())
                        .phoneNumber(employee.getPhoneNumber())
                        .gender(employee.getGender())
                        .dateOfBirth(employee.getDateOfBirth())
                        .ritId(employee.getRitId())
                        .csxRacfId(employee.getCsxRacfId())
                        .employmentType(employee.getEmploymentType())
                        .workMode(employee.getWorkMode())
                        .vendorId(employee.getVendor() == null ? null : employee.getVendor().getId())
                        .vendorCompanyName(employee.getVendor() == null ? null : employee.getVendor().getCompanyName())
                        .designationId(employee.getDesignationId())
                        .designationName(lookupName(lookupValues, employee.getDesignationId()))
                        .status(employee.getStatus())
                        .addressDetails(addressResponse(employee.getId()))
                        .compensationDetails(compensationResponse(employee.getId()))
                        .professionalDetails(professionalDetailsResponse(employee.getId()))
                        .bankDetails(bankDetailsResponse(employee.getId()))
                        .documentList(documentResponses(employee))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeHierarchyResponse getHierarchy(Long employeeId, String roleType, Long cycleId) {
        Employee viewer = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        PerformanceCycles cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found: " + cycleId));
        String normalizedRole = normalizeHierarchyRole(roleType);

        List<Employee> allEmployees = employeeRepository.findAll();
        Map<Long, Employee> employees = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, EmployeeAssignment> assignments = assignmentRepository
                .findByStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDesc("ACTIVE").stream()
                .collect(Collectors.toMap(EmployeeAssignment::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, EmployeeRole> roles = employeeRoleRepository.findByIsCurrentTrue().stream()
                .sorted(Comparator.comparing(EmployeeRole::getEffectiveFrom).reversed())
                .collect(Collectors.toMap(EmployeeRole::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, LookupValue> lookups = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Sow> sows = sowRepository.findAll().stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));

        validateHierarchyRole(normalizedRole, roles.get(employeeId), lookups);
        List<Long> visibleEmployeeIds = switch (normalizedRole) {
            case "MANAGER" -> directManagerReportIds(employeeId, assignments);
            case "TEAM_LEAD" -> directLeadReportIds(employeeId, assignments);
            default -> List.of(employeeId);
        };
        Map<Long, EmployeeReview> reviews = employeeReviewRepository
                .findByPerformanceCycleIdAndEmployeeIdIn(cycleId, visibleEmployeeIds).stream()
                .collect(Collectors.toMap(review -> review.getEmployee().getId(), Function.identity()));

        List<EmployeeHierarchyMemberResponse> reports = hierarchyMembers(visibleEmployeeIds, employeeId,
                assignments, roles, employees, lookups, sows, reviews);
        return EmployeeHierarchyResponse.builder()
                .viewerEmployeeId(employeeId).viewerEmployeeName(employeeName(viewer))
                .roleType(normalizedRole).cycleId(cycleId).cycleName(cycle.getCycleName())
                .employees(reports).build();
    }

    private List<EmployeeHierarchyMemberResponse> hierarchyMembers(List<Long> employeeIds, Long viewerEmployeeId,
            Map<Long, EmployeeAssignment> assignments, Map<Long, EmployeeRole> roles,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookups, Map<Long, Sow> sows,
            Map<Long, EmployeeReview> reviews) {
        return employeeIds.stream().map(employees::get).filter(Objects::nonNull)
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> hierarchyMember(employee, viewerEmployeeId, assignments, roles,
                        employees, lookups, sows, reviews))
                .toList();
    }

    private EmployeeHierarchyMemberResponse hierarchyMember(Employee employee, Long viewerEmployeeId,
            Map<Long, EmployeeAssignment> assignments, Map<Long, EmployeeRole> roles,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookups, Map<Long, Sow> sows,
            Map<Long, EmployeeReview> reviews) {
        if (employee == null) return null;
        EmployeeAssignment assignment = assignments.get(employee.getId());
        EmployeeRole employeeRole = roles.get(employee.getId());
        LookupValue role = employeeRole == null ? null : lookups.get(employeeRole.getRoleId());
        LookupValue department = assignment == null ? null : lookups.get(assignment.getDepartmentId());
        Long profileDesignationId = employee.getDesignationId() != null
                ? employee.getDesignationId()
                : assignment == null ? null : assignment.getDesignationId();
        LookupValue designation = lookups.get(profileDesignationId);
        Sow sow = assignment == null ? null : sows.get(assignment.getSowId());
        SowMilestone milestone = assignment == null || assignment.getMilestoneId() == null
                ? null : sowMilestoneRepository.findById(assignment.getMilestoneId()).orElse(null);
        Employee manager = assignment == null ? null : employees.get(assignment.getManagerId());
        Employee lead = assignment == null ? null : employees.get(assignment.getLeadId());
        EmployeeReview review = reviews.get(employee.getId());
        List<EmployeeReviewAssessment> assessments = review == null ? List.of() : review.getAssessments();
        EmployeeReviewAssessment self = assessmentForRole(assessments, "EMPLOYEE");
        EmployeeReviewAssessment teamLead = assessmentForRole(assessments, "TEAM_LEAD");
        EmployeeReviewAssessment managerAssessment = assessmentForRole(assessments, "MANAGER");
        EmployeeReviewAssessment assigned = assessments.stream()
                .filter(assessment -> assessment.getAssessorEmployee() != null
                        && viewerEmployeeId.equals(assessment.getAssessorEmployee().getId()))
                .filter(assessment -> assessment.getStatus() != EmployeeReviewStatus.SUBMITTED)
                .min(Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel)).orElse(null);
        boolean actionRequired = assigned != null && assessments.stream()
                .filter(assessment -> assessment.getAssessmentLevel() < assigned.getAssessmentLevel())
                .allMatch(assessment -> assessment.getStatus() == EmployeeReviewStatus.SUBMITTED);

        return EmployeeHierarchyMemberResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employeeName(employee)).email(employee.getEmail()).phoneNumber(employee.getPhoneNumber())
                .gender(employee.getGender()).dateOfBirth(employee.getDateOfBirth())
                .ritId(employee.getRitId()).csxRacfId(employee.getCsxRacfId())
                .roleId(employeeRole == null ? null : employeeRole.getRoleId())
                .roleName(role == null ? null : role.getName())
                .departmentId(assignment == null ? null : assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .designationId(profileDesignationId)
                .designationName(designation == null ? null : designation.getName())
                .sowId(assignment == null ? null : assignment.getSowId())
                .sowName(sow == null ? null : sow.getSowName())
                .milestoneId(assignment == null ? null : assignment.getMilestoneId())
                .milestoneName(assignment == null ? null
                        : milestone == null ? "All milestones" : milestone.getMilestoneName())
                .positionType(assignment == null ? null : assignment.getPositionType())
                .isPrimaryAssignment(assignment == null ? null : assignment.getIsPrimaryAssignment())
                .managerId(assignment == null ? null : assignment.getManagerId())
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(assignment == null ? null : assignment.getLeadId())
                .leadName(lead == null ? null : employeeName(lead)).status(employee.getStatus())
                .addressDetails(addressResponse(employee.getId()))
                .compensationDetails(compensationResponse(employee.getId()))
                .professionalDetails(professionalDetailsResponse(employee.getId()))
                .bankDetails(bankDetailsResponse(employee.getId()))
                .documentList(documentResponses(employee))
                .reviewId(review == null ? null : review.getId())
                .reviewStatus(review == null ? null : review.getStatus())
                .reviewProgressPercentage(review == null ? null : review.getProgressPercentage())
                .selfAssessmentStatus(self == null ? null : self.getStatus())
                .teamLeadAssessmentStatus(teamLead == null ? null : teamLead.getStatus())
                .managerAssessmentStatus(managerAssessment == null ? null : managerAssessment.getStatus())
                .assignedAssessmentId(assigned == null ? null : assigned.getId())
                .assignedRoleName(assigned == null || assigned.getAssessorRole() == null
                        ? null : assigned.getAssessorRole().getName())
                .assignedAssessmentStatus(assigned == null ? null : assigned.getStatus())
                .actionRequired(actionRequired).build();
    }

    private EmployeeReviewAssessment assessmentForRole(List<EmployeeReviewAssessment> assessments, String roleType) {
        return assessments.stream().filter(assessment -> assessment.getAssessorRole() != null)
                .filter(assessment -> roleType.equals(normalizeAssessmentRole(assessment.getAssessorRole())))
                .findFirst().orElse(null);
    }

    private String normalizeAssessmentRole(LookupValue role) {
        String value = role.getCode() == null || role.getCode().isBlank() ? role.getName() : role.getCode();
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.equals("SELF") || normalized.equals("SELFASSESSOR")) return "EMPLOYEE";
        if (normalized.equals("TL") || normalized.equals("LEAD") || normalized.equals("TEAMLEAD")) return "TEAM_LEAD";
        return normalized;
    }

    private List<Long> directLeadReportIds(Long leadId, Map<Long, EmployeeAssignment> assignments) {
        return assignments.values().stream().filter(assignment -> leadId.equals(assignment.getLeadId()))
                .map(EmployeeAssignment::getEmployeeId).distinct().toList();
    }

    private List<Long> directManagerReportIds(Long managerId, Map<Long, EmployeeAssignment> assignments) {
        return assignments.values().stream().filter(assignment -> managerId.equals(assignment.getManagerId()))
                .map(EmployeeAssignment::getEmployeeId).distinct().toList();
    }

    private String normalizeHierarchyRole(String roleType) {
        if (roleType == null || roleType.isBlank()) throw new InvalidOperationException("roleType is required");
        String normalized = roleType.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return switch (normalized) {
            case "EMPLOYEE" -> "EMPLOYEE";
            case "TL", "LEAD", "TEAMLEAD" -> "TEAM_LEAD";
            case "MANAGER" -> "MANAGER";
            default -> throw new InvalidOperationException("Unsupported roleType: " + roleType);
        };
    }

    private void validateHierarchyRole(String requestedRole, EmployeeRole currentRole,
            Map<Long, LookupValue> lookups) {
        LookupValue role = currentRole == null ? null : lookups.get(currentRole.getRoleId());
        if (role == null || !requestedRole.equals(normalizeAssessmentRole(role)))
            throw new InvalidOperationException("roleType does not match the employee's current role");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportingManagerResponse> getReportingManagers(Long sowId, Long departmentId,
            Long designationId, Long excludeEmployeeId) {
        List<Long> employeeIds = assignmentRepository.findByStatusIgnoreCase("ACTIVE").stream()
                .filter(assignment -> sowId == null || sowId.equals(assignment.getSowId()))
                .filter(assignment -> departmentId == null || departmentId.equals(assignment.getDepartmentId()))
                .filter(assignment -> designationId == null || designationId.equals(assignment.getDesignationId()))
                .map(EmployeeAssignment::getEmployeeId)
                .filter(id -> excludeEmployeeId == null || !excludeEmployeeId.equals(id))
                .distinct().toList();
        if (employeeIds.isEmpty()) return List.of();

        return employeeRepository.findByIdIn(employeeIds).stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> ReportingManagerResponse.builder()
                        .employeeId(employee.getId())
                        .employeeName(employeeName(employee)).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DirectReportsResponse getDirectReports(Long managerEmployeeId) {
        Employee manager = employeeRepository.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager employee not found: " + managerEmployeeId));
        Map<Long, EmployeeAssignment> assignmentsByEmployee = java.util.stream.Stream.concat(
                assignmentRepository.findByLeadIdAndStatusIgnoreCase(managerEmployeeId, "ACTIVE").stream(),
                assignmentRepository.findByManagerIdAndStatusIgnoreCase(managerEmployeeId, "ACTIVE").stream())
                .collect(Collectors.toMap(EmployeeAssignment::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        if (assignmentsByEmployee.isEmpty()) {
            return DirectReportsResponse.builder().managerEmployeeId(managerEmployeeId)
                    .managerEmployeeName(employeeName(manager)).totalReports(0).employees(List.of()).build();
        }

        List<Employee> reports = employeeRepository.findByIdIn(assignmentsByEmployee.keySet().stream().toList())
                .stream().filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Long, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Sow> sows = sowRepository.findAll().stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        Map<Long, EmployeeRole> currentRoles = employeeRoleRepository.findByIsCurrentTrue().stream()
                .sorted(Comparator.comparing(EmployeeRole::getEffectiveFrom).reversed())
                .collect(Collectors.toMap(EmployeeRole::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, EmployeeReviewAssessment> latestReviews = employeeReviewAssessmentRepository
                .findSelfAssessmentsForEmployeesOrderByUpdatedDateDesc(
                        reports.stream().map(Employee::getId).toList())
                .stream().collect(Collectors.toMap(
                        assessment -> assessment.getEmployeeReview().getEmployee().getId(),
                        Function.identity(), (first, ignored) -> first));
        List<EmployeeBasicInfoResponse> responses = reports.stream()
                .map(employee -> toResponse(employee, assignmentsByEmployee.get(employee.getId()),
                        currentRoles.get(employee.getId()), employees, lookupValues, sows,
                        latestReviews.get(employee.getId())))
                .toList();
        return DirectReportsResponse.builder().managerEmployeeId(managerEmployeeId)
                .managerEmployeeName(employeeName(manager)).totalReports(responses.size())
                .employees(responses).build();
    }

    @Override
    @Transactional
    public EmployeeBasicInfoResponse update(Long employeeId, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        updateEmployee(employee, request);
        saveAddress(employee, request.getAddressDetails());
        saveCompensation(employee, request.getCompensationDetails());
        saveProfessionalDetails(employee, request.getProfessionalDetails());
        saveBankDetails(employee, request.getBankDetails());
        if (request.getDocumentList() != null) {
            synchronizeDocuments(employee, request.getDocumentList());
        }

        EmployeeAssignment assignment = assignmentRepository.findActiveByEmployeeId(employeeId)
                .orElse(null);
        boolean reactivatingSowAssignment = false;
        Long requestedSowId = requestedSowId(request);
        if (requestedSowId != null
                && (assignment == null || !requestedSowId.equals(assignment.getSowId()))) {
            EmployeeAssignment sowAssignment = assignmentRepository
                    .findFirstBySowIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(
                            requestedSowId, employeeId)
                    .orElse(null);
            if (sowAssignment != null) {
                assignment = sowAssignment;
                reactivatingSowAssignment = !"ACTIVE".equalsIgnoreCase(sowAssignment.getStatus());
            }
        }
        boolean hasAssignmentUpdate = request.getProjectAssignment() != null;
        if (hasAssignmentUpdate)
            assignment = replaceAssignmentWhenChanged(
                    employeeId, assignment, request, reactivatingSowAssignment);

        employeeRepository.save(employee);
        LookupValue role = updateEmployeeRoleWhenChanged(employee, request);
        ensureEmployeeUser(employee, role);
        if (hasAssignmentUpdate && assignment != null) {
            seedReviewsForNewEmployee(employee, request.getUpdatedBy(), assignment);
        }
        return currentEmployeeResponse(employee, assignment);
    }

    private void ensureEmployeeUser(Employee employee, LookupValue role) {
        String username = employee.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmployeeId(employee.getId()).orElse(null);
        if (user == null) {
            if (userRepository.existsByUsernameIgnoreCase(username))
                throw new InvalidOperationException("Username already exists: " + username);
            user = new User();
            user.setUsername(username);
            user.setPassword(DEFAULT_PASSWORD);
            user.setStatus("ACTIVE");
            user.setRole(role);
            user.setEmployee(employee);
        } else if (!user.getUsername().equalsIgnoreCase(username)) {
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, user.getId()))
                throw new InvalidOperationException("Username already exists: " + username);
            user.setUsername(username);
        }
        user.setRole(role);
        userRepository.save(user);
    }

    private LookupValue employeeRoleLookup() {
        return lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "ROLE", DEFAULT_ROLE)
                .orElseThrow(() -> new InvalidOperationException("EMPLOYEE role lookup is not configured"));
    }

    private LookupValue requestedEmployeeRole(Long roleId) {
        if (roleId == null) return employeeRoleLookup();
        return requireLookup(roleId, "ROLE");
    }

    private Long resolveProfileDesignationId(EmployeeCreateRequest request) {
        Long designationId = request.getDesignationId();
        if (designationId == null && request.getProjectAssignment() != null) {
            designationId = request.getProjectAssignment().getDesignationId();
        }
        if (designationId == null) {
            throw new InvalidOperationException("designationId is required");
        }
        return requireLookup(designationId, "DESIGNATION").getId();
    }

    private EmployeeRole createEmployeeRole(Employee employee, LookupValue role,
            LocalDate effectiveFrom, Long actorId) {
        EmployeeRole mapping = new EmployeeRole();
        mapping.setEmployeeId(employee.getId());
        mapping.setRoleId(role.getId());
        mapping.setEffectiveFrom(effectiveFrom == null ? LocalDate.now() : effectiveFrom);
        mapping.setIsCurrent(true);
        mapping.setCreatedBy(actorId);
        mapping.setUpdatedBy(actorId);
        return employeeRoleRepository.save(mapping);
    }

    private LookupValue updateEmployeeRoleWhenChanged(Employee employee, EmployeeUpdateRequest request) {
        EmployeeRole current = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employee.getId())
                .orElse(null);
        if (!request.isRoleIdPresent()) {
            if (current != null) return requireLookup(current.getRoleId(), "ROLE");
            LookupValue defaultRole = employeeRoleLookup();
            createEmployeeRole(employee, defaultRole, LocalDate.now(), request.getUpdatedBy());
            return defaultRole;
        }
        if (request.getRoleId() == null)
            throw new InvalidOperationException("roleId cannot be null");
        LookupValue requestedRole = requireLookup(request.getRoleId(), "ROLE");
        if (current != null && requestedRole.getId().equals(current.getRoleId())) return requestedRole;

        LocalDate effectiveFrom = request.getProjectAssignment() != null
                && request.getProjectAssignment().getEffectiveFrom() != null
                ? request.getProjectAssignment().getEffectiveFrom()
                : LocalDate.now();
        if (current != null) {
            validateEffectiveDate(effectiveFrom, current.getEffectiveFrom(), "role");
            current.setEffectiveTo(effectiveFrom);
            current.setIsCurrent(false);
            current.setUpdatedBy(request.getUpdatedBy());
            employeeRoleRepository.save(current);
        }
        createEmployeeRole(employee, requestedRole, effectiveFrom, request.getUpdatedBy());
        return requestedRole;
    }

    private void updateEmployee(Employee employee, EmployeeUpdateRequest request) {
        if (request.getFirstName() != null) {
            String firstName = request.getFirstName().trim();
            if (firstName.isEmpty()) throw new InvalidOperationException("firstName cannot be blank");
            employee.setFirstName(firstName);
        }
        if (request.getLastName() != null) employee.setLastName(request.getLastName().trim());
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, employee.getId()))
                throw new InvalidOperationException("Employee email already exists: " + email);
            employee.setEmail(email);
        }
        if (request.getPhoneNumber() != null) employee.setPhoneNumber(request.getPhoneNumber().trim());
        if (request.getGender() != null) employee.setGender(normalizeGender(request.getGender()));
        if (request.getDateOfBirth() != null) employee.setDateOfBirth(request.getDateOfBirth());
        if (request.getRitId() != null) {
            String ritId = normalizeIdentifier(request.getRitId());
            validateIdentifiersAvailable(ritId, null, employee.getId());
            employee.setRitId(ritId);
        }
        if (request.getCsxRacfId() != null) {
            String csxRacfId = normalizeIdentifier(request.getCsxRacfId());
            validateIdentifiersAvailable(null, csxRacfId, employee.getId());
            employee.setCsxRacfId(csxRacfId);
        }
        if (request.getEmploymentType() != null)
            employee.setEmploymentType(normalizeRequiredValue(request.getEmploymentType(), "employmentType"));
        if (request.getJoiningDate() != null)
            employee.setJoiningDate(request.getJoiningDate());
        if (request.getWorkMode() != null)
            employee.setWorkMode(normalizeRequiredValue(request.getWorkMode(), "workMode"));
        if (request.isVendorIdPresent() || request.getEmploymentType() != null) {
            Long vendorId = request.isVendorIdPresent() ? request.getVendorId()
                    : employee.getVendor() == null ? null : employee.getVendor().getId();
            employee.setVendor(resolveVendor(vendorId, employee.getEmploymentType()));
        }
        if (request.isDesignationIdPresent()) {
            if (request.getDesignationId() == null) {
                throw new InvalidOperationException("designationId cannot be null");
            }
            employee.setDesignationId(requireLookup(request.getDesignationId(), "DESIGNATION").getId());
        }
        if (request.getStatus() != null) employee.setStatus(request.getStatus().trim().toUpperCase());
        employee.setUpdatedBy(request.getUpdatedBy());
    }

    private EmployeeAssignment replaceAssignmentWhenChanged(Long employeeId, EmployeeAssignment current,
            EmployeeUpdateRequest request, boolean reactivating) {
        ProjectAssignmentRequest nested = request.getProjectAssignment();
        Long designationId = nested.isDesignationIdPresent() ? nested.getDesignationId()
                : current == null ? null : current.getDesignationId();
        Long leadId = nested.isLeadIdPresent() ? nested.getLeadId()
                : current == null ? null : current.getLeadId();
        Long managerId = nested.isManagerIdPresent() ? nested.getManagerId()
                : current == null ? null : current.getManagerId();
        Long sowId = nested.isSowIdPresent() ? nested.getSowId()
                : current == null ? null : current.getSowId();
        Long departmentId = sowId == null
                ? nested.isDepartmentIdPresent() ? nested.getDepartmentId()
                        : current == null ? null : current.getDepartmentId()
                : departmentIdForSow(sowId);
        Long milestoneId = nested.isMilestoneIdPresent() ? nested.getMilestoneId()
                : current == null ? null : current.getMilestoneId();
        String positionType = nested.isPositionTypePresent() ? normalizePositionType(nested.getPositionType())
                : current == null ? null : current.getPositionType();
        Integer allocationPercentage = nested.getAllocationPercentage() != null
                ? nested.getAllocationPercentage()
                : current == null || current.getAllocationPercentage() == null ? 100 : current.getAllocationPercentage();
        LocalDate assignmentEndDate = nested.getAssignmentEndDate() != null
                ? nested.getAssignmentEndDate() : current == null ? null : current.getEffectiveTo();
        String assignmentStatus = nested.getStatus() != null
                ? normalizeAssignmentStatus(nested.getStatus())
                : current == null || current.getStatus() == null ? "ACTIVE" : current.getStatus();
        if (reactivating) {
            assignmentEndDate = null;
            assignmentStatus = "ACTIVE";
        }
        validateAssignmentValues(employeeId, departmentId, designationId, sowId, milestoneId,
                leadId, managerId);

        LocalDate effectiveFrom = nested.getEffectiveFrom() != null
                ? nested.getEffectiveFrom()
                : LocalDate.now();
        validateAssignmentDates(effectiveFrom, assignmentEndDate);
        if (reactivating) {
            current.setDepartmentId(departmentId);
            current.setDesignationId(designationId);
            current.setLeadId(leadId);
            current.setManagerId(managerId);
            current.setSowId(sowId);
            current.setMilestoneId(milestoneId);
            current.setPositionType(positionType);
            current.setAllocationPercentage(allocationPercentage);
            current.setEffectiveFrom(effectiveFrom);
            current.setEffectiveTo(null);
            current.setStatus("ACTIVE");
            current.setIsPrimaryAssignment(Boolean.TRUE.equals(current.getIsPrimaryAssignment()));
            current.setUpdatedBy(request.getUpdatedBy());
            return assignmentRepository.save(current);
        }

        boolean changed = current == null
                || !Objects.equals(current.getDepartmentId(), departmentId)
                || !Objects.equals(current.getDesignationId(), designationId)
                || !Objects.equals(current.getLeadId(), leadId)
                || !Objects.equals(current.getManagerId(), managerId)
                || !Objects.equals(current.getSowId(), sowId)
                || !Objects.equals(current.getMilestoneId(), milestoneId)
                || !Objects.equals(current.getPositionType(), positionType)
                || !Objects.equals(current.getAllocationPercentage(), allocationPercentage)
                || !Objects.equals(current.getEffectiveTo(), assignmentEndDate)
                || !Objects.equals(current.getStatus(), assignmentStatus);
        if (!changed) return current;

        if (current != null) {
            validateEffectiveDate(effectiveFrom, current.getEffectiveFrom(), "assignment");
            current.setEffectiveTo(effectiveFrom);
            current.setStatus("INACTIVE");
            current.setUpdatedBy(request.getUpdatedBy());
            assignmentRepository.save(current);
        }

        EmployeeAssignment replacement = new EmployeeAssignment();
        replacement.setEmployeeId(employeeId);
        replacement.setDepartmentId(departmentId);
        replacement.setDesignationId(designationId);
        replacement.setLeadId(leadId);
        replacement.setManagerId(managerId);
        replacement.setSowId(sowId);
        replacement.setMilestoneId(milestoneId);
        replacement.setPositionType(positionType);
        replacement.setEffectiveFrom(effectiveFrom);
        replacement.setEffectiveTo(assignmentEndDate);
        replacement.setAllocationPercentage(allocationPercentage);
        replacement.setStatus(assignmentStatus);
        replacement.setIsPrimaryAssignment(current == null
                || Boolean.TRUE.equals(current.getIsPrimaryAssignment()));
        replacement.setCreatedBy(request.getUpdatedBy());
        replacement.setUpdatedBy(request.getUpdatedBy());
        return assignmentRepository.save(replacement);
    }

    private Long requestedSowId(EmployeeUpdateRequest request) {
        ProjectAssignmentRequest nested = request.getProjectAssignment();
        if (nested != null && nested.isSowIdPresent()) return nested.getSowId();
        return null;
    }

    private void validateAssignmentValues(Long employeeId, Long departmentId, Long designationId,
            Long sowId, Long milestoneId, Long leadId, Long managerId) {
        if (departmentId != null) requireLookup(departmentId, "DEPARTMENT");
        if (designationId != null) requireLookup(designationId, "DESIGNATION");
        if (sowId != null && !sowRepository.existsById(sowId))
            throw new ResourceNotFoundException("SOW not found: " + sowId);
        if (milestoneId != null && sowId == null)
            throw new InvalidOperationException("sowId is required when milestoneId is provided");
        if (milestoneId != null && sowMilestoneRepository.findByIdAndSow_Id(milestoneId, sowId).isEmpty())
            throw new ResourceNotFoundException(
                    "Milestone " + milestoneId + " not found for SOW " + sowId);
        validateSupervisor(employeeId, leadId, "Team Lead");
        validateSupervisor(employeeId, managerId, "Manager");
        if (leadId != null && leadId.equals(managerId))
            throw new InvalidOperationException("Team Lead and Manager must be different employees");
        validateReportingHierarchy(employeeId, leadId);
        validateReportingHierarchy(employeeId, managerId);
    }

    private Long departmentIdForSow(Long sowId) {
        Sow sow = sowRepository.findById(sowId)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + sowId));
        if (sow.getBusinessUnit() == null || sow.getBusinessUnit().getId() == null) {
            throw new InvalidOperationException("SOW " + sowId + " does not have a business unit");
        }
        Long departmentId = sow.getBusinessUnit().getId();
        requireLookup(departmentId, "DEPARTMENT");
        return departmentId;
    }

    private void validateSupervisor(Long employeeId, Long supervisorId, String label) {
        if (supervisorId == null) return;
        if (employeeId.equals(supervisorId))
            throw new InvalidOperationException("Employee cannot be their own " + label.toLowerCase());
        Employee supervisor = employeeRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " employee not found: " + supervisorId));
        if (!"ACTIVE".equalsIgnoreCase(supervisor.getStatus()))
            throw new InvalidOperationException(label + " employee is not active: " + supervisorId);
    }

    private void validateReportingHierarchy(Long employeeId, Long supervisorId) {
        java.util.Set<Long> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<Long> pending = new java.util.ArrayDeque<>();
        if (supervisorId != null) pending.add(supervisorId);
        while (!pending.isEmpty()) {
            Long currentId = pending.removeFirst();
            if (employeeId.equals(currentId))
                throw new InvalidOperationException("Reporting assignment would create a circular hierarchy");
            if (!visited.add(currentId)) continue;
            assignmentRepository.findActiveByEmployeeId(currentId).ifPresent(assignment -> {
                if (assignment.getLeadId() != null) pending.add(assignment.getLeadId());
                if (assignment.getManagerId() != null) pending.add(assignment.getManagerId());
            });
        }
    }

    private void validateEffectiveDate(LocalDate effectiveFrom, LocalDate currentEffectiveFrom, String type) {
        if (currentEffectiveFrom != null && effectiveFrom.isBefore(currentEffectiveFrom))
            throw new InvalidOperationException("New " + type + " effective date cannot be before the current effective date");
    }

    private LookupValue requireLookup(Long lookupId, String expectedType) {
        LookupValue lookup = lookupValueRepository.findById(lookupId)
                .orElseThrow(() -> new ResourceNotFoundException(expectedType + " lookup not found: " + lookupId));
        if (lookup.getLookupType() == null || !expectedType.equalsIgnoreCase(lookup.getLookupType().getCode()))
            throw new InvalidOperationException("Lookup " + lookupId + " is not a " + expectedType);
        return lookup;
    }

    private String lookupName(Map<Long, LookupValue> lookupValues, Long lookupId) {
        LookupValue value = lookupId == null ? null : lookupValues.get(lookupId);
        return value == null ? null : value.getName();
    }

    private EmployeeBasicInfoResponse currentEmployeeResponse(Employee employee, EmployeeAssignment assignment) {
        Map<Long, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Sow> sows = sowRepository.findAll().stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        List<EmployeeAssignment> assignments = assignmentRepository.findByEmployeeId(employee.getId()).stream()
                .sorted(assignmentDisplayOrder()).toList();
        Map<Long, SowMilestone> milestones = sowMilestoneRepository.findAllById(assignments.stream()
                        .map(EmployeeAssignment::getMilestoneId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(SowMilestone::getId, Function.identity()));
        EmployeeRole role = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employee.getId()).orElse(null);
        return toResponse(employee, assignment, role, employees, lookupValues, sows, null,
                assignments, milestones);
    }

    private EmployeeBasicInfoResponse toResponse(Employee employee, EmployeeAssignment assignment, EmployeeRole role,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Sow> sows) {
        return toResponse(employee, assignment, role, employees, lookupValues, sows, null);
    }

    private EmployeeBasicInfoResponse toResponse(Employee employee, EmployeeAssignment assignment, EmployeeRole role,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Sow> sows,
            EmployeeReviewAssessment review) {
        Map<Long, SowMilestone> milestones = assignment == null || assignment.getMilestoneId() == null
                ? Map.of()
                : sowMilestoneRepository.findById(assignment.getMilestoneId()).stream()
                        .collect(Collectors.toMap(SowMilestone::getId, Function.identity()));
        return toResponse(employee, assignment, role, employees, lookupValues, sows, review,
                assignment == null ? List.of() : List.of(assignment), milestones);
    }

    private EmployeeBasicInfoResponse toResponse(Employee employee, EmployeeAssignment assignment, EmployeeRole role,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Sow> sows,
            EmployeeReviewAssessment review, List<EmployeeAssignment> assignments,
        Map<Long, SowMilestone> milestones) {
        Long profileDesignationId = employee.getDesignationId() != null
                ? employee.getDesignationId()
                : assignment == null ? null : assignment.getDesignationId();
        LookupValue designation = lookupValues.get(profileDesignationId);
        LookupValue department = assignment == null ? null : lookupValues.get(assignment.getDepartmentId());
        LookupValue roleLookup = role == null ? null : lookupValues.get(role.getRoleId());
        Sow sow = assignment == null ? null : sows.get(assignment.getSowId());
        SowMilestone milestone = assignment == null || assignment.getMilestoneId() == null
                ? null : milestones.get(assignment.getMilestoneId());
        Employee manager = assignment == null ? null : employees.get(assignment.getManagerId());
        Employee lead = assignment == null ? null : employees.get(assignment.getLeadId());

        return EmployeeBasicInfoResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employeeName(employee))
                .firstName(employee.getFirstName()).lastName(employee.getLastName())
                .email(employee.getEmail()).phoneNumber(employee.getPhoneNumber())
                .gender(employee.getGender()).dateOfBirth(employee.getDateOfBirth())
                .ritId(employee.getRitId()).csxRacfId(employee.getCsxRacfId())
                .employmentType(employee.getEmploymentType())
                .joiningDate(employee.getJoiningDate())
                .workMode(employee.getWorkMode())
                .vendorId(employee.getVendor() == null ? null : employee.getVendor().getId())
                .vendorCompanyName(employee.getVendor() == null ? null : employee.getVendor().getCompanyName())
                .roleId(role == null ? null : role.getRoleId())
                .roleCode(roleLookup == null ? null : roleLookup.getCode())
                .roleName(roleLookup == null ? null : roleLookup.getName())
                .designationId(profileDesignationId)
                .designationName(designation == null ? null : designation.getName())
                .assignmentId(assignment == null ? null : assignment.getId())
                .sowId(assignment == null ? null : assignment.getSowId())
                .sowCode(sow == null ? null : sow.getSowCode())
                .sowName(sow == null ? null : sow.getSowName())
                .milestoneId(assignment == null ? null : assignment.getMilestoneId())
                .milestoneName(assignment == null ? null
                        : milestone == null ? "All milestones" : milestone.getMilestoneName())
                .positionType(assignment == null ? null : assignment.getPositionType())
                .isPrimaryAssignment(assignment == null ? null : assignment.getIsPrimaryAssignment())
                .allocationPercentage(assignment == null ? null : assignment.getAllocationPercentage())
                .assignmentStartDate(assignment == null ? null : assignment.getEffectiveFrom())
                .assignmentEndDate(assignment == null ? null : assignment.getEffectiveTo())
                .assignmentStatus(assignment == null ? null
                        : assignment.getStatus() == null
                        ? "INACTIVE"
                        : assignment.getStatus())
                .departmentId(assignment == null ? null : assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .managerId(assignment == null ? null : assignment.getManagerId())
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(assignment == null ? null : assignment.getLeadId())
                .leadName(lead == null ? null : employeeName(lead))
                .assignmentList(assignments.stream()
                        .sorted(assignmentDisplayOrder())
                        .map(item -> assignmentResponse(item, employees, lookupValues, sows, milestones))
                        .toList())
                .status(employee.getStatus())
                .addressDetails(addressResponse(employee.getId()))
                .compensationDetails(compensationResponse(employee.getId()))
                .professionalDetails(professionalDetailsResponse(employee.getId()))
                .bankDetails(bankDetailsResponse(employee.getId()))
                .documentList(documentResponses(employee))
                .review(review == null ? null : EmployeeReviewSummaryResponse.builder()
                        .status(review.getStatus()).updatedOn(review.getUpdatedDate())
                        .progressPercentage(review.getProgressPercentage()).build())
                .build();
    }

    private EmployeeAssignmentResponse assignmentResponse(EmployeeAssignment assignment,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Sow> sows,
            Map<Long, SowMilestone> milestones) {
        LookupValue designation = lookupValues.get(assignment.getDesignationId());
        LookupValue department = lookupValues.get(assignment.getDepartmentId());
        Sow sow = sows.get(assignment.getSowId());
        SowMilestone milestone = assignment.getMilestoneId() == null
                ? null : milestones.get(assignment.getMilestoneId());
        Employee manager = employees.get(assignment.getManagerId());
        Employee lead = employees.get(assignment.getLeadId());
        return EmployeeAssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .sowId(assignment.getSowId())
                .sowCode(sow == null ? null : sow.getSowCode())
                .sowName(sow == null ? null : sow.getSowName())
                .milestoneId(assignment.getMilestoneId())
                .milestoneName(assignment.getMilestoneId() == null
                        ? "All milestones" : milestone == null ? null : milestone.getMilestoneName())
                .designationId(assignment.getDesignationId())
                .designationName(designation == null ? null : designation.getName())
                .positionType(assignment.getPositionType())
                .isPrimaryAssignment(assignment.getIsPrimaryAssignment())
                .allocationPercentage(assignment.getAllocationPercentage())
                .assignmentStartDate(assignment.getEffectiveFrom())
                .assignmentEndDate(assignment.getEffectiveTo())
                .assignmentStatus(assignment.getStatus() == null ? "INACTIVE" : assignment.getStatus())
                .departmentId(assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .managerId(assignment.getManagerId())
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(assignment.getLeadId())
                .leadName(lead == null ? null : employeeName(lead))
                .build();
    }

    private static Comparator<EmployeeAssignment> assignmentDisplayOrder() {
        return Comparator
                .comparing((EmployeeAssignment assignment) ->
                        Boolean.TRUE.equals(assignment.getIsPrimaryAssignment())).reversed()
                .thenComparing(assignment -> "ACTIVE".equalsIgnoreCase(assignment.getStatus()),
                        Comparator.reverseOrder())
                .thenComparing(EmployeeAssignment::getEffectiveFrom,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(EmployeeAssignment::getId,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private static String normalizeIdentifier(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new InvalidOperationException(fieldName + " cannot be blank");
        return value.trim().toUpperCase().replace(' ', '_');
    }

    private static String normalizeAssignmentStatus(String value) {
        if (value == null || value.isBlank()) return "ACTIVE";
        String normalized = value.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized))
            throw new InvalidOperationException("assignment status must be ACTIVE or INACTIVE");
        return normalized;
    }

    private static void validateAssignmentDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate))
            throw new InvalidOperationException("assignmentEndDate cannot be before assignmentStartDate");
    }

    private Vendor resolveVendor(Long vendorId, String employmentType) {
        boolean contractEmployee = "CONTRACT".equalsIgnoreCase(employmentType);
        if (vendorId == null) {
            if (contractEmployee)
                throw new InvalidOperationException("vendorId is required for contract employees");
            return null;
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));
        if (!"ACTIVE".equalsIgnoreCase(vendor.getStatus()))
            throw new InvalidOperationException("Vendor is inactive: " + vendorId);
        return vendor;
    }

    private void validateIdentifiersAvailable(String ritId, String csxRacfId, Long excludedEmployeeId) {
        boolean ritIdExists = ritId != null && (excludedEmployeeId == null
                ? employeeRepository.existsByRitIdIgnoreCase(ritId)
                : employeeRepository.existsByRitIdIgnoreCaseAndIdNot(ritId, excludedEmployeeId));
        if (ritIdExists) throw new InvalidOperationException("RIT ID already exists: " + ritId);

        boolean csxRacfIdExists = csxRacfId != null && (excludedEmployeeId == null
                ? employeeRepository.existsByCsxRacfIdIgnoreCase(csxRacfId)
                : employeeRepository.existsByCsxRacfIdIgnoreCaseAndIdNot(csxRacfId, excludedEmployeeId));
        if (csxRacfIdExists)
            throw new InvalidOperationException("CSX RACF ID already exists: " + csxRacfId);
    }

    private String formatRitEmployeeId(Long employeeId) {
        return String.format("RIT%02d", employeeId);
    }

    private void saveAddress(Employee employee, EmployeeAddressRequest request) {
        if (request == null) return;
        EmployeeAddress address = employeeAddressRepository.findByEmployeeId(employee.getId())
                .orElseGet(() -> EmployeeAddress.builder().employee(employee).build());
        address.setAddressLine1(trimToNull(request.getAddressLine1()));
        address.setAddressLine2(trimToNull(request.getAddressLine2()));
        address.setCity(trimToNull(request.getCity()));
        address.setState(trimToNull(request.getState()));
        address.setPostalCode(trimToNull(request.getPostalCode()));
        address.setCountry(trimToNull(request.getCountry()));
        employeeAddressRepository.save(address);
    }

    private EmployeeAddressResponse addressResponse(Long employeeId) {
        return employeeAddressRepository.findByEmployeeId(employeeId)
                .map(address -> EmployeeAddressResponse.builder()
                        .id(address.getId())
                        .addressLine1(address.getAddressLine1())
                        .addressLine2(address.getAddressLine2())
                        .city(address.getCity())
                        .state(address.getState())
                        .postalCode(address.getPostalCode())
                        .country(address.getCountry())
                        .build())
                .orElse(null);
    }

    private void saveCompensation(Employee employee, EmployeeCompensationRequest request) {
        if (request == null) return;
        String payType = request.getPayType().trim().toUpperCase();
        if (!Set.of("HOURLY", "SALARY").contains(payType)) {
            throw new InvalidOperationException("payType must be HOURLY or SALARY");
        }
        String currency = request.getCurrency().trim().toUpperCase();
        String reason = trimToNull(request.getReason());
        EmployeeCompensation current = employeeCompensationRepository
                .findFirstByEmployeeIdAndCurrentTrueOrderByEffectiveDateDescIdDesc(employee.getId())
                .orElse(null);
        if (current != null
                && payType.equals(current.getPayType())
                && request.getHourlyRate().compareTo(current.getHourlyRate()) == 0
                && currency.equals(current.getCurrency())
                && request.getEffectiveDate().equals(current.getEffectiveDate())) {
            if (!Objects.equals(current.getReason(), reason)) {
                current.setReason(reason);
                employeeCompensationRepository.save(current);
            }
            return;
        }
        if (current != null) {
            current.setCurrent(false);
            employeeCompensationRepository.save(current);
        }
        employeeCompensationRepository.save(EmployeeCompensation.builder()
                .employee(employee)
                .payType(payType)
                .hourlyRate(request.getHourlyRate())
                .currency(currency)
                .effectiveDate(request.getEffectiveDate())
                .reason(reason)
                .current(true)
                .build());
    }

    private EmployeeCompensationResponse compensationResponse(Long employeeId) {
        return employeeCompensationRepository
                .findFirstByEmployeeIdAndCurrentTrueOrderByEffectiveDateDescIdDesc(employeeId)
                .map(compensation -> EmployeeCompensationResponse.builder()
                        .id(compensation.getId())
                        .payType(compensation.getPayType())
                        .hourlyRate(compensation.getHourlyRate())
                        .currency(compensation.getCurrency())
                        .effectiveDate(compensation.getEffectiveDate())
                        .build())
                .orElse(null);
    }

    private void saveProfessionalDetails(Employee employee, EmployeeProfessionalDetailsRequest request) {
        if (request == null) return;
        EmployeeProfessionalProfile profile = employeeProfessionalProfileRepository.findByEmployeeId(employee.getId())
                .orElseGet(() -> EmployeeProfessionalProfile.builder().employee(employee).build());
        profile.setItSkills(trimToNull(request.getItSkills()));
        profile.setLatestExperience(trimToNull(request.getLatestExperience()));
        employeeProfessionalProfileRepository.save(profile);
    }

    private EmployeeProfessionalDetailsResponse professionalDetailsResponse(Long employeeId) {
        return employeeProfessionalProfileRepository.findByEmployeeId(employeeId)
                .map(profile -> EmployeeProfessionalDetailsResponse.builder()
                        .id(profile.getId())
                        .itSkills(profile.getItSkills())
                        .latestExperience(profile.getLatestExperience())
                        .build())
                .orElse(null);
    }

    private void saveBankDetails(Employee employee, EmployeeBankDetailsRequest request) {
        if (request == null) return;
        BankAccount account = bankAccountRepository
                .findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                        BankAccountOwnerType.EMPLOYEE, employee.getId())
                .orElseGet(() -> BankAccount.builder()
                        .ownerType(BankAccountOwnerType.EMPLOYEE)
                        .ownerId(employee.getId())
                        .isPrimary(true)
                        .active(true)
                        .build());
        account.setBankCountry(request.getBankCountry().trim());
        account.setCurrency(request.getCurrency().trim().toUpperCase());
        account.setAccountHolderName(request.getAccountHolderName().trim());
        account.setBankName(request.getBankName().trim());
        account.setIfscCode(request.getIfscCode().trim().toUpperCase());
        account.setPaymentMethod("BANK_TRANSFER");
        String accountNumber = trimToNull(request.getAccountNumber());
        if (account.getId() == null && accountNumber == null) {
            throw new InvalidOperationException("accountNumber is required for new bank details");
        }
        if (accountNumber != null) {
            account.setAccountNumberEncrypted(accountNumber);
            account.setAccountNumberLast4(accountNumber.substring(Math.max(0, accountNumber.length() - 4)));
        }
        bankAccountRepository.save(account);
    }

    private EmployeeBankDetailsResponse bankDetailsResponse(Long employeeId) {
        return bankAccountRepository
                .findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                        BankAccountOwnerType.EMPLOYEE, employeeId)
                .map(account -> EmployeeBankDetailsResponse.builder()
                        .id(account.getId())
                        .bankCountry(account.getBankCountry())
                        .currency(account.getCurrency())
                        .accountHolderName(account.getAccountHolderName())
                        .bankName(account.getBankName())
                        .accountNumberLast4(account.getAccountNumberLast4())
                        .ifscCode(account.getIfscCode())
                        .build())
                .orElse(null);
    }

    private void synchronizeDocuments(Employee employee, List<EmployeeDocumentRequest> documentList) {
        if (documentList == null) return;
        List<Long> requestedIds = documentList.stream().map(EmployeeDocumentRequest::getId).toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidOperationException("documentList cannot contain duplicate document ids");
        }
        List<Document> documents = uniqueIds.isEmpty() ? List.of() : documentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(uniqueIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Documents not found: " + missingIds);
        }
        for (Document document : documents) {
            if (!"EMPLOYEE".equalsIgnoreCase(document.getModule())) {
                throw new InvalidOperationException("Document " + document.getId() + " is not an employee document");
            }
            String type = document.getDocumentType() == null ? "" : document.getDocumentType().toUpperCase();
            if (!Set.of("RESUME", "EDUCATIONAL_DOCUMENT").contains(type)) {
                throw new InvalidOperationException(
                        "Employee document type must be RESUME or EDUCATIONAL_DOCUMENT");
            }
        }
        employee.getDocuments().clear();
        employee.getDocuments().addAll(documents);
        employeeRepository.save(employee);
    }

    private List<DocumentResponse> documentResponses(Employee employee) {
        return employee.getDocuments().stream()
                .sorted(Comparator.comparing(Document::getId))
                .map(document -> DocumentResponse.builder()
                        .id(document.getId())
                        .documentName(document.getDocumentName())
                        .fileType(document.getFileType())
                        .documentType(document.getDocumentType())
                        .fileUrl(document.getFileUrl())
                        .module(document.getModule())
                        .uploadedAt(document.getUploadedAt())
                        .build())
                .toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) return null;
        String normalized = gender.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (!ALLOWED_GENDERS.contains(normalized)) {
            throw new InvalidOperationException(
                    "gender must be one of MALE, FEMALE, NON_BINARY, OTHER, PREFER_NOT_TO_SAY");
        }
        return normalized;
    }

    private String normalizePositionType(String positionType) {
        if (positionType == null || positionType.isBlank()) return null;
        String normalized = positionType.trim().toUpperCase().replace(' ', '_');
        if ("NONBILLABLE".equals(normalized)) normalized = "NON_BILLABLE";
        if (!Set.of("BILLABLE", "NON_BILLABLE").contains(normalized)) {
            throw new InvalidOperationException("positionType must be BILLABLE or NON_BILLABLE");
        }
        return normalized;
    }

    private void clearPrimaryAssignment(Long employeeId, Long updatedBy) {
        List<EmployeeAssignment> activeAssignments =
                assignmentRepository.findAllByEmployeeIdAndStatusIgnoreCase(employeeId, "ACTIVE");
        activeAssignments.stream()
                .filter(assignment -> Boolean.TRUE.equals(assignment.getIsPrimaryAssignment()))
                .forEach(assignment -> {
                    assignment.setIsPrimaryAssignment(false);
                    assignment.setUpdatedBy(updatedBy);
                });
        assignmentRepository.saveAll(activeAssignments);
    }
}
