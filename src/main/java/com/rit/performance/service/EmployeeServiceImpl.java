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
import com.rit.performance.dto.ProjectAssignmentRequest;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.EmployeeRole;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.entity.Projects;
import com.rit.performance.entity.User;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.ProjectRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final EmployeeReviewAssessmentRepository employeeReviewAssessmentRepository;
    private final EmployeeReviewRepository employeeReviewRepository;
    private final PerformanceCycleConfigRepository cycleRepository;

    @Override
    @Transactional
    public EmployeeCreateResponse create(EmployeeCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(email) || userRepository.existsByUsernameIgnoreCase(email))
            throw new InvalidOperationException("Employee email or username already exists: " + email);
        String ritId = normalizeIdentifier(request.getRitId());
        String csxRacfId = normalizeIdentifier(request.getCsxRacfId());
        validateIdentifiersAvailable(ritId, csxRacfId, null);
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName() == null ? null : request.getLastName().trim());
        employee.setEmail(email);
        employee.setPhoneNumber(request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim());
        employee.setRitId(ritId);
        employee.setCsxRacfId(csxRacfId);
        employee.setJoiningDate(request.getJoiningDate());
        employee.setEmploymentType(request.getEmploymentType() == null ? null : request.getEmploymentType().trim());
        employee.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus().trim().toUpperCase());
        employee.setCreatedBy(request.getCreatedBy());
        employee.setUpdatedBy(request.getCreatedBy());
        employee = employeeRepository.save(employee);
        EmployeeAssignment assignment = createInitialAssignment(employee.getId(), request);
        LookupValue role = requestedEmployeeRole(request.getRoleId());
        createEmployeeRole(employee, role, employee.getJoiningDate(), request.getCreatedBy());
        User user = new User();
        user.setUsername(email);
        user.setPassword(DEFAULT_PASSWORD);
        user.setStatus("ACTIVE");
        user.setRole(role);
        user.setEmployee(employee);
        user = userRepository.save(user);

        return EmployeeCreateResponse.builder().employee(currentEmployeeResponse(employee, assignment))
                .userId(user.getId()).username(user.getUsername()).password(DEFAULT_PASSWORD)
                .roleName(role.getName()).build();
    }

    private EmployeeAssignment createInitialAssignment(Long employeeId, EmployeeCreateRequest request) {
        ProjectAssignmentRequest nested = request.getProjectAssignment();
        Long departmentId = nested == null ? request.getDepartmentId() : nested.getDepartmentId();
        Long projectId = nested == null ? request.getProjectId() : nested.getProjectId();
        Long leadId = nested == null ? null : nested.getLeadId();
        Long managerId = nested == null ? request.getManagerId() : nested.getManagerId();
        LocalDate effectiveFrom = nested != null && nested.getEffectiveFrom() != null
                ? nested.getEffectiveFrom() : request.getAssignmentEffectiveFrom();
        boolean hasAssignment = departmentId != null || request.getDesignationId() != null
                || leadId != null || managerId != null || projectId != null || effectiveFrom != null;
        if (!hasAssignment) return null;
        validateAssignmentValues(employeeId, departmentId, request.getDesignationId(), projectId, leadId, managerId);

        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployeeId(employeeId);
        assignment.setDepartmentId(departmentId);
        assignment.setDesignationId(request.getDesignationId());
        assignment.setLeadId(leadId);
        assignment.setManagerId(managerId);
        assignment.setProjectId(projectId);
        assignment.setEffectiveFrom(effectiveFrom == null ? request.getJoiningDate() : effectiveFrom);
        assignment.setIsCurrent(true);
        assignment.setCreatedBy(request.getCreatedBy());
        assignment.setUpdatedBy(request.getCreatedBy());
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeBasicInfoResponse> getBasicInfo() {
        List<Employee> employees = employeeRepository.findAll();
        Map<Long, Employee> employeesById = employees.stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, EmployeeAssignment> currentAssignments = assignmentRepository.findByIsCurrentTrue().stream()
                .collect(Collectors.toMap(EmployeeAssignment::getEmployeeId, Function.identity(), (first, ignored) -> first));
        Map<Long, EmployeeRole> currentRoles = employeeRoleRepository.findByIsCurrentTrue().stream()
                .sorted(Comparator.comparing(EmployeeRole::getEffectiveFrom).reversed())
                .collect(Collectors.toMap(EmployeeRole::getEmployeeId, Function.identity(), (first, ignored) -> first));
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Projects> projects = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Projects::getId, Function.identity()));

        return employees.stream()
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> toResponse(employee, currentAssignments.get(employee.getId()),
                        currentRoles.get(employee.getId()), employeesById, lookupValues, projects))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeInformationResponse> getEmployeeInformation() {
        return employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> EmployeeInformationResponse.builder()
                        .employeeId(employee.getId())
                        .employeeName(employeeName(employee))
                        .firstName(employee.getFirstName())
                        .lastName(employee.getLastName())
                        .email(employee.getEmail())
                        .phoneNumber(employee.getPhoneNumber())
                        .ritId(employee.getRitId())
                        .csxRacfId(employee.getCsxRacfId())
                        .joiningDate(employee.getJoiningDate())
                        .employmentType(employee.getEmploymentType())
                        .status(employee.getStatus())
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
        Map<Long, EmployeeAssignment> assignments = assignmentRepository.findByIsCurrentTrue().stream()
                .collect(Collectors.toMap(EmployeeAssignment::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, EmployeeRole> roles = employeeRoleRepository.findByIsCurrentTrue().stream()
                .sorted(Comparator.comparing(EmployeeRole::getEffectiveFrom).reversed())
                .collect(Collectors.toMap(EmployeeRole::getEmployeeId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, LookupValue> lookups = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Projects> projects = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Projects::getId, Function.identity()));

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
                assignments, roles, employees, lookups, projects, reviews);
        return EmployeeHierarchyResponse.builder()
                .viewerEmployeeId(employeeId).viewerEmployeeName(employeeName(viewer))
                .roleType(normalizedRole).cycleId(cycleId).cycleName(cycle.getCycleName())
                .employees(reports).build();
    }

    private List<EmployeeHierarchyMemberResponse> hierarchyMembers(List<Long> employeeIds, Long viewerEmployeeId,
            Map<Long, EmployeeAssignment> assignments, Map<Long, EmployeeRole> roles,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookups, Map<Long, Projects> projects,
            Map<Long, EmployeeReview> reviews) {
        return employeeIds.stream().map(employees::get).filter(Objects::nonNull)
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .sorted(Comparator.comparing(EmployeeServiceImpl::employeeName, String.CASE_INSENSITIVE_ORDER))
                .map(employee -> hierarchyMember(employee, viewerEmployeeId, assignments, roles,
                        employees, lookups, projects, reviews))
                .toList();
    }

    private EmployeeHierarchyMemberResponse hierarchyMember(Employee employee, Long viewerEmployeeId,
            Map<Long, EmployeeAssignment> assignments, Map<Long, EmployeeRole> roles,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookups, Map<Long, Projects> projects,
            Map<Long, EmployeeReview> reviews) {
        if (employee == null) return null;
        EmployeeAssignment assignment = assignments.get(employee.getId());
        EmployeeRole employeeRole = roles.get(employee.getId());
        LookupValue role = employeeRole == null ? null : lookups.get(employeeRole.getRoleId());
        LookupValue department = assignment == null ? null : lookups.get(assignment.getDepartmentId());
        LookupValue designation = assignment == null ? null : lookups.get(assignment.getDesignationId());
        Projects project = assignment == null ? null : projects.get(assignment.getProjectId());
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
                .ritId(employee.getRitId()).csxRacfId(employee.getCsxRacfId())
                .roleId(employeeRole == null ? null : employeeRole.getRoleId())
                .roleName(role == null ? null : role.getName())
                .departmentId(assignment == null ? null : assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .designationId(assignment == null ? null : assignment.getDesignationId())
                .designationName(designation == null ? null : designation.getName())
                .projectId(assignment == null ? null : assignment.getProjectId())
                .projectName(project == null ? null : project.getProjectName())
                .managerId(assignment == null ? null : assignment.getManagerId())
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(assignment == null ? null : assignment.getLeadId())
                .leadName(lead == null ? null : employeeName(lead)).status(employee.getStatus())
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
    public List<ReportingManagerResponse> getReportingManagers(Long projectId, Long departmentId,
            Long designationId, Long excludeEmployeeId) {
        List<Long> employeeIds = assignmentRepository
                .findByProjectIdAndDepartmentIdAndDesignationIdAndIsCurrentTrue(
                        projectId, departmentId, designationId).stream()
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
                assignmentRepository.findByLeadIdAndIsCurrentTrue(managerEmployeeId).stream(),
                assignmentRepository.findByManagerIdAndIsCurrentTrue(managerEmployeeId).stream())
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
        Map<Long, Projects> projects = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Projects::getId, Function.identity()));
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
                        currentRoles.get(employee.getId()), employees, lookupValues, projects,
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

        EmployeeAssignment assignment = assignmentRepository.findByEmployeeIdAndIsCurrentTrue(employeeId)
                .orElse(null);
        boolean hasAssignmentUpdate = request.getProjectAssignment() != null
                || request.isDepartmentIdPresent() || request.isDesignationIdPresent()
                || request.isManagerIdPresent() || request.isProjectIdPresent()
                || request.getAssignmentEffectiveFrom() != null;
        if (hasAssignmentUpdate)
            assignment = replaceAssignmentWhenChanged(employeeId, assignment, request);

        employeeRepository.save(employee);
        LookupValue role = updateEmployeeRoleWhenChanged(employee, request);
        ensureEmployeeUser(employee, role);
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
                : request.getAssignmentEffectiveFrom() == null ? LocalDate.now() : request.getAssignmentEffectiveFrom();
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
        if (request.getJoiningDate() != null) employee.setJoiningDate(request.getJoiningDate());
        if (request.getEmploymentType() != null) employee.setEmploymentType(request.getEmploymentType().trim());
        if (request.getStatus() != null) employee.setStatus(request.getStatus().trim().toUpperCase());
        employee.setUpdatedBy(request.getUpdatedBy());
    }

    private EmployeeAssignment replaceAssignmentWhenChanged(Long employeeId, EmployeeAssignment current,
            EmployeeUpdateRequest request) {
        ProjectAssignmentRequest nested = request.getProjectAssignment();
        Long departmentId = nested != null && nested.isDepartmentIdPresent() ? nested.getDepartmentId()
                : request.isDepartmentIdPresent() ? request.getDepartmentId()
                : current == null ? null : current.getDepartmentId();
        Long designationId = request.isDesignationIdPresent() ? request.getDesignationId()
                : current == null ? null : current.getDesignationId();
        Long leadId = nested != null && nested.isLeadIdPresent() ? nested.getLeadId()
                : current == null ? null : current.getLeadId();
        Long managerId = nested != null && nested.isManagerIdPresent() ? nested.getManagerId()
                : request.isManagerIdPresent() ? request.getManagerId()
                : current == null ? null : current.getManagerId();
        Long projectId = nested != null && nested.isProjectIdPresent() ? nested.getProjectId()
                : request.isProjectIdPresent() ? request.getProjectId()
                : current == null ? null : current.getProjectId();
        validateAssignmentValues(employeeId, departmentId, designationId, projectId, leadId, managerId);

        boolean changed = current == null
                || !Objects.equals(current.getDepartmentId(), departmentId)
                || !Objects.equals(current.getDesignationId(), designationId)
                || !Objects.equals(current.getLeadId(), leadId)
                || !Objects.equals(current.getManagerId(), managerId)
                || !Objects.equals(current.getProjectId(), projectId);
        if (!changed) return current;

        LocalDate effectiveFrom = nested != null && nested.getEffectiveFrom() != null
                ? nested.getEffectiveFrom()
                : request.getAssignmentEffectiveFrom() == null ? LocalDate.now() : request.getAssignmentEffectiveFrom();
        if (current != null) {
            validateEffectiveDate(effectiveFrom, current.getEffectiveFrom(), "assignment");
            current.setEffectiveTo(effectiveFrom);
            current.setIsCurrent(false);
            current.setUpdatedBy(request.getUpdatedBy());
            assignmentRepository.save(current);
        }

        EmployeeAssignment replacement = new EmployeeAssignment();
        replacement.setEmployeeId(employeeId);
        replacement.setDepartmentId(departmentId);
        replacement.setDesignationId(designationId);
        replacement.setLeadId(leadId);
        replacement.setManagerId(managerId);
        replacement.setProjectId(projectId);
        replacement.setEffectiveFrom(effectiveFrom);
        replacement.setIsCurrent(true);
        replacement.setCreatedBy(request.getUpdatedBy());
        replacement.setUpdatedBy(request.getUpdatedBy());
        return assignmentRepository.save(replacement);
    }

    private void validateAssignmentValues(Long employeeId, Long departmentId, Long designationId,
            Long projectId, Long leadId, Long managerId) {
        if (departmentId != null) requireLookup(departmentId, "DEPARTMENT");
        if (designationId != null) requireLookup(designationId, "DESIGNATION");
        if (projectId != null && !projectRepository.existsById(projectId))
            throw new ResourceNotFoundException("Project not found: " + projectId);
        validateSupervisor(employeeId, leadId, "LEAD", "Team Lead");
        validateSupervisor(employeeId, managerId, "MANAGER", "Manager");
        if (leadId != null && leadId.equals(managerId))
            throw new InvalidOperationException("Team Lead and Manager must be different employees");
        validateReportingHierarchy(employeeId, leadId);
        validateReportingHierarchy(employeeId, managerId);
    }

    private void validateSupervisor(Long employeeId, Long supervisorId, String expectedRole, String label) {
        if (supervisorId == null) return;
        if (employeeId.equals(supervisorId))
            throw new InvalidOperationException("Employee cannot be their own " + label.toLowerCase());
        Employee supervisor = employeeRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " employee not found: " + supervisorId));
        if (!"ACTIVE".equalsIgnoreCase(supervisor.getStatus()))
            throw new InvalidOperationException(label + " employee is not active: " + supervisorId);
        EmployeeRole currentRole = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(supervisorId)
                .orElseThrow(() -> new InvalidOperationException(label + " has no current role: " + supervisorId));
        LookupValue role = requireLookup(currentRole.getRoleId(), "ROLE");
        String normalized = normalizeAssessmentRole(role);
        if (!(expectedRole.equals(normalized) || ("LEAD".equals(expectedRole) && "TEAM_LEAD".equals(normalized))))
            throw new InvalidOperationException(label + " employee does not have the required role: " + supervisorId);
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
            assignmentRepository.findByEmployeeIdAndIsCurrentTrue(currentId).ifPresent(assignment -> {
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

    private EmployeeBasicInfoResponse currentEmployeeResponse(Employee employee, EmployeeAssignment assignment) {
        Map<Long, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAll().stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        Map<Long, Projects> projects = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Projects::getId, Function.identity()));
        EmployeeRole role = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employee.getId()).orElse(null);
        return toResponse(employee, assignment, role, employees, lookupValues, projects);
    }

    private EmployeeBasicInfoResponse toResponse(Employee employee, EmployeeAssignment assignment, EmployeeRole role,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Projects> projects) {
        return toResponse(employee, assignment, role, employees, lookupValues, projects, null);
    }

    private EmployeeBasicInfoResponse toResponse(Employee employee, EmployeeAssignment assignment, EmployeeRole role,
            Map<Long, Employee> employees, Map<Long, LookupValue> lookupValues, Map<Long, Projects> projects,
            EmployeeReviewAssessment review) {
        LookupValue designation = assignment == null ? null : lookupValues.get(assignment.getDesignationId());
        LookupValue department = assignment == null ? null : lookupValues.get(assignment.getDepartmentId());
        LookupValue roleLookup = role == null ? null : lookupValues.get(role.getRoleId());
        Projects project = assignment == null ? null : projects.get(assignment.getProjectId());
        Employee manager = assignment == null ? null : employees.get(assignment.getManagerId());
        Employee lead = assignment == null ? null : employees.get(assignment.getLeadId());

        return EmployeeBasicInfoResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employeeName(employee))
                .firstName(employee.getFirstName()).lastName(employee.getLastName())
                .email(employee.getEmail()).phoneNumber(employee.getPhoneNumber())
                .ritId(employee.getRitId()).csxRacfId(employee.getCsxRacfId())
                .joiningDate(employee.getJoiningDate()).employmentType(employee.getEmploymentType())
                .roleId(role == null ? null : role.getRoleId())
                .roleCode(roleLookup == null ? null : roleLookup.getCode())
                .roleName(roleLookup == null ? null : roleLookup.getName())
                .designationId(assignment == null ? null : assignment.getDesignationId())
                .designationName(designation == null ? null : designation.getName())
                .projectId(assignment == null ? null : assignment.getProjectId())
                .projectName(project == null ? null : project.getProjectName())
                .departmentId(assignment == null ? null : assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .managerId(assignment == null ? null : assignment.getManagerId())
                .managerName(manager == null ? null : employeeName(manager))
                .leadId(assignment == null ? null : assignment.getLeadId())
                .leadName(lead == null ? null : employeeName(lead))
                .status(employee.getStatus())
                .review(review == null ? null : EmployeeReviewSummaryResponse.builder()
                        .status(review.getStatus()).updatedOn(review.getUpdatedDate())
                        .progressPercentage(review.getProgressPercentage()).build())
                .build();
    }

    private static String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private static String normalizeIdentifier(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
}
