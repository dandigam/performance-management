package com.rit.performance.service;

import com.rit.performance.dto.ProjectEmployeeResponse;
import com.rit.performance.dto.ProjectEmployeesResponse;
import com.rit.performance.dto.ProjectEmployeeCreateRequest;
import com.rit.performance.dto.ProjectEmployeeStatusUpdateRequest;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Projects;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectsService {

    private final ProjectRepository repository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LookupValueRepository lookupValueRepository;

    @Override
    public Projects save(Projects project) {
        return repository.save(project);
    }

    @Override
    public Projects update(Long id, Projects project) {

        Projects dbProject = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        dbProject.setProjectCode(project.getProjectCode());
        dbProject.setProjectName(project.getProjectName());
        dbProject.setDescription(project.getDescription());
        dbProject.setStartDate(project.getStartDate());
        dbProject.setEndDate(project.getEndDate());
        dbProject.setStatus(project.getStatus());
        dbProject.setDepartmentId(project.getDepartmentId());
        dbProject.setUpdatedBy(project.getUpdatedBy());

        return repository.save(dbProject);
    }

    @Override
    public Projects getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Override
    public List<Projects> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectEmployeesResponse getEmployees(Long projectId, int page, int size) {
        Projects project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("effectiveFrom"), Sort.Order.asc("id")));
        Page<EmployeeAssignment> assignments =
                assignmentRepository.findBySowIdAndStatusIgnoreCase(projectId, "ACTIVE", pageable);

        List<Long> employeeIds = assignments.getContent().stream()
                .map(EmployeeAssignment::getEmployeeId).distinct().toList();
        Map<Long, Employee> employees = employeeRepository.findByIdIn(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<Long> lookupIds = java.util.stream.Stream.concat(
                assignments.getContent().stream()
                        .flatMap(assignment -> java.util.stream.Stream.of(
                                assignment.getDesignationId(), assignment.getDepartmentId())),
                java.util.stream.Stream.of(project.getDepartmentId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, LookupValue> lookupValues = lookupValueRepository.findAllById(lookupIds).stream()
                .collect(Collectors.toMap(LookupValue::getId, Function.identity()));

        List<ProjectEmployeeResponse> roster = assignments.getContent().stream()
                .map(assignment -> toProjectEmployee(assignment, employees.get(assignment.getEmployeeId()),
                        lookupValues.get(assignment.getDesignationId()),
                        lookupValues.get(assignment.getDepartmentId())))
                .toList();
        return ProjectEmployeesResponse.builder()
                .projectId(project.getId())
                .projectCode(project.getProjectCode())
                .projectName(project.getProjectName())
                .departmentId(project.getDepartmentId())
                .departmentName(project.getDepartmentId() == null ? null
                        : lookupValues.get(project.getDepartmentId()) == null ? null
                        : lookupValues.get(project.getDepartmentId()).getName())
                .employees(roster)
                .page(assignments.getNumber())
                .size(assignments.getSize())
                .totalElements(assignments.getTotalElements())
                .totalPages(assignments.getTotalPages())
                .first(assignments.isFirst())
                .last(assignments.isLast())
                .build();
    }

    @Override
    @Transactional
    public ProjectEmployeeResponse addEmployee(Long projectId, ProjectEmployeeCreateRequest request) {
        Projects project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus()))
            throw new InvalidOperationException("Employee is not active: " + employee.getId());
        if (assignmentRepository.existsBySowIdAndEmployeeIdAndStatusIgnoreCase(
                projectId, employee.getId(), "ACTIVE"))
            throw new DuplicateResourceException(
                    "Employee " + employee.getId() + " is already assigned to project " + projectId);
        validateAssignmentStartDate(project, request.getAssignmentStartDate());
        String status = normalizeAssignmentStatus(request.getStatus());
        if (!"ACTIVE".equals(status))
            throw new InvalidOperationException(
                    "status must be ACTIVE when adding an employee to a project");

        EmployeeAssignment previous = assignmentRepository
                .findFirstBySowIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(
                        projectId, employee.getId())
                .orElse(null);
        if (previous != null && "ACTIVE".equals(status)) {
            previous.setEffectiveFrom(request.getAssignmentStartDate());
            previous.setEffectiveTo(null);
            previous.setStatus("ACTIVE");
            if (!assignmentRepository
                    .existsByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrue(employee.getId(), "ACTIVE")) {
                previous.setIsPrimaryAssignment(true);
            }
            if (previous.getAllocationPercentage() == null)
                previous.setAllocationPercentage(100);
            previous = assignmentRepository.save(previous);
            LookupValue designation = previous.getDesignationId() == null ? null
                    : lookupValueRepository.findById(previous.getDesignationId()).orElse(null);
            LookupValue department = previous.getDepartmentId() == null ? null
                    : lookupValueRepository.findById(previous.getDepartmentId()).orElse(null);
            return toProjectEmployee(previous, employee, designation, department);
        }

        EmployeeAssignment current = assignmentRepository
                .findActiveByEmployeeId(employee.getId())
                .orElseGet(() -> assignmentRepository
                        .findFirstByEmployeeIdOrderByEffectiveFromDescIdDesc(employee.getId())
                        .orElse(null));
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployeeId(employee.getId());
        assignment.setSowId(projectId);
        assignment.setDepartmentId(current == null ? null : current.getDepartmentId());
        assignment.setDesignationId(current == null ? null : current.getDesignationId());
        assignment.setManagerId(current == null ? null : current.getManagerId());
        assignment.setLeadId(current == null ? null : current.getLeadId());
        assignment.setAllocationPercentage(100);
        assignment.setEffectiveFrom(request.getAssignmentStartDate());
        assignment.setStatus(status);
        assignment.setIsPrimaryAssignment("ACTIVE".equals(status)
                && !assignmentRepository
                        .existsByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrue(employee.getId(), "ACTIVE"));
        assignment = assignmentRepository.save(assignment);

        LookupValue designation = assignment.getDesignationId() == null ? null
                : lookupValueRepository.findById(assignment.getDesignationId()).orElse(null);
        LookupValue department = assignment.getDepartmentId() == null ? null
                : lookupValueRepository.findById(assignment.getDepartmentId()).orElse(null);
        return toProjectEmployee(assignment, employee, designation, department);
    }

    @Override
    @Transactional
    public ProjectEmployeeResponse updateEmployeeAssignmentStatus(
            Long projectId, Long assignmentId, ProjectEmployeeStatusUpdateRequest request) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        EmployeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(value -> projectId.equals(value.getSowId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment " + assignmentId + " not found for project " + projectId));
        Long employeeId = assignment.getEmployeeId();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId));
        String status = normalizeAssignmentStatus(request.getStatus());

        if ("ACTIVE".equals(status)) {
            if (!"ACTIVE".equalsIgnoreCase(employee.getStatus()))
                throw new InvalidOperationException("Employee is not active: " + employee.getId());
            if (assignmentRepository.existsBySowIdAndEmployeeIdAndStatusIgnoreCaseAndIdNot(
                    projectId, employee.getId(), "ACTIVE", assignmentId))
                throw new DuplicateResourceException(
                        "Employee " + employee.getId() + " is already assigned to project " + projectId);
            assignment.setEffectiveTo(null);
        } else {
            assignment.setIsPrimaryAssignment(false);
            if (assignment.getEffectiveTo() == null) {
                LocalDate today = LocalDate.now();
                assignment.setEffectiveTo(assignment.getEffectiveFrom() != null
                        && assignment.getEffectiveFrom().isAfter(today)
                        ? assignment.getEffectiveFrom() : today);
            }
        }
        assignment.setStatus(status);
        assignment = assignmentRepository.save(assignment);

        LookupValue designation = assignment.getDesignationId() == null ? null
                : lookupValueRepository.findById(assignment.getDesignationId()).orElse(null);
        LookupValue department = assignment.getDepartmentId() == null ? null
                : lookupValueRepository.findById(assignment.getDepartmentId()).orElse(null);
        return toProjectEmployee(assignment, employee, designation, department);
    }

    private void validateAssignmentStartDate(Projects project, java.time.LocalDate startDate) {
        if (project.getStartDate() != null && startDate.isBefore(project.getStartDate()))
            throw new InvalidOperationException("assignmentStartDate cannot be before the project start date");
        if (project.getEndDate() != null && startDate.isAfter(project.getEndDate()))
            throw new InvalidOperationException("assignmentStartDate cannot be after the project end date");
    }

    private String normalizeAssignmentStatus(String value) {
        if (value == null || value.isBlank()) return "ACTIVE";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized))
            throw new InvalidOperationException("status must be ACTIVE or INACTIVE");
        return normalized;
    }

    private ProjectEmployeeResponse toProjectEmployee(
            EmployeeAssignment assignment, Employee employee,
            LookupValue designation, LookupValue department) {
        return ProjectEmployeeResponse.builder()
                .assignmentId(assignment.getId())
                .employeeId(assignment.getEmployeeId())
                .employeeNumber(employee == null ? null : employee.getRitId())
                .employeeName(employee == null ? null : employeeName(employee))
                .email(employee == null ? null : employee.getEmail())
                .designationId(assignment.getDesignationId())
                .designationName(designation == null ? null : designation.getName())
                .departmentId(assignment.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .workMode(employee == null ? null : employee.getWorkMode())
                .allocationPercentage(assignment.getAllocationPercentage())
                .assignmentStartDate(assignment.getEffectiveFrom())
                .assignmentEndDate(assignment.getEffectiveTo())
                .status(assignment.getStatus() == null ? "INACTIVE" : assignment.getStatus())
                .build();
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
