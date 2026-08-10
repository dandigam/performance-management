package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Projects;
import com.rit.performance.dto.ProjectEmployeeCreateRequest;
import com.rit.performance.dto.ProjectEmployeeStatusUpdateRequest;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceImplTest {

    @Test
    void reactivatesExistingAssignmentWhenEmployeeIsReadded() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);
        Employee employee = new Employee();
        employee.setId(12L);
        employee.setFirstName("Dinakar");
        employee.setStatus("ACTIVE");
        EmployeeAssignment historical = new EmployeeAssignment();
        historical.setId(101L);
        historical.setEmployeeId(12L);
        historical.setProjectId(1L);
        historical.setDesignationId(4L);
        historical.setDepartmentId(48L);
        historical.setStatus("INACTIVE");
        historical.setIsCurrent(false);
        historical.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        historical.setEffectiveTo(LocalDate.of(2026, 8, 1));
        ProjectEmployeeCreateRequest request = new ProjectEmployeeCreateRequest();
        request.setEmployeeId(12L);
        request.setAssignmentStartDate(LocalDate.of(2026, 8, 9));

        when(projects.findById(1L)).thenReturn(Optional.of(project()));
        when(employees.findById(12L)).thenReturn(Optional.of(employee));
        when(assignments.findByEmployeeIdAndIsCurrentTrue(12L)).thenReturn(Optional.empty());
        when(assignments.findFirstByProjectIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(1L, 12L))
                .thenReturn(Optional.of(historical));
        when(assignments.save(historical)).thenReturn(historical);

        var response = service.addEmployee(1L, request);

        assertEquals(101L, response.getAssignmentId());
        assertEquals(4L, response.getDesignationId());
        assertEquals("ACTIVE", response.getStatus());
        assertTrue(historical.getIsCurrent());
        assertEquals(LocalDate.of(2026, 8, 9), historical.getEffectiveFrom());
        assertNull(historical.getEffectiveTo());
    }

    @Test
    void inactivatesProjectAssignmentWithoutDeletingIt() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setId(101L);
        assignment.setProjectId(1L);
        assignment.setEmployeeId(12L);
        assignment.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        assignment.setIsCurrent(true);
        assignment.setStatus("ACTIVE");
        Employee employee = new Employee();
        employee.setId(12L);
        employee.setFirstName("Dinakar");
        employee.setStatus("ACTIVE");
        ProjectEmployeeStatusUpdateRequest request = new ProjectEmployeeStatusUpdateRequest();
        request.setStatus("INACTIVE");

        when(projects.findById(1L)).thenReturn(Optional.of(project()));
        when(assignments.findById(101L)).thenReturn(Optional.of(assignment));
        when(employees.findById(12L)).thenReturn(Optional.of(employee));
        when(assignments.save(assignment)).thenReturn(assignment);

        var response = service.updateEmployeeAssignmentStatus(1L, 101L, request);

        assertEquals("INACTIVE", response.getStatus());
        assertFalse(assignment.getIsCurrent());
        assertNotNull(assignment.getEffectiveTo());
    }

    @Test
    void addsEmployeeToProjectWithCurrentEmployeeContext() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);
        Employee employee = new Employee();
        employee.setId(12L);
        employee.setFirstName("Dinakar");
        employee.setLastName("Kalaga");
        employee.setRitId("RIT-0012");
        employee.setWorkMode("ONSITE");
        employee.setStatus("ACTIVE");
        EmployeeAssignment current = new EmployeeAssignment();
        current.setDepartmentId(48L);
        current.setDesignationId(4L);
        current.setManagerId(5L);
        current.setLeadId(2L);
        ProjectEmployeeCreateRequest request = new ProjectEmployeeCreateRequest();
        request.setEmployeeId(12L);
        request.setAssignmentStartDate(LocalDate.of(2026, 8, 9));
        request.setStatus("ACTIVE");

        when(projects.findById(1L)).thenReturn(Optional.of(project()));
        when(employees.findById(12L)).thenReturn(Optional.of(employee));
        when(assignments.findByEmployeeIdAndIsCurrentTrue(12L)).thenReturn(Optional.of(current));
        when(assignments.save(any(EmployeeAssignment.class))).thenAnswer(invocation -> {
            EmployeeAssignment saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        var response = service.addEmployee(1L, request);

        assertEquals(101L, response.getAssignmentId());
        assertEquals(12L, response.getEmployeeId());
        assertEquals("ONSITE", response.getWorkMode());
        assertEquals(100, response.getAllocationPercentage());
        assertEquals(4L, response.getDesignationId());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void rejectsDuplicateCurrentProjectAssignment() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);
        Employee employee = new Employee();
        employee.setId(12L);
        employee.setStatus("ACTIVE");
        ProjectEmployeeCreateRequest request = new ProjectEmployeeCreateRequest();
        request.setEmployeeId(12L);
        request.setAssignmentStartDate(LocalDate.of(2026, 8, 9));

        when(projects.findById(1L)).thenReturn(Optional.of(project()));
        when(employees.findById(12L)).thenReturn(Optional.of(employee));
        when(assignments.existsByProjectIdAndEmployeeIdAndIsCurrentTrue(1L, 12L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.addEmployee(1L, request));
    }

    @Test
    void returnsPagedProjectEmployeesWithAssignmentDetails() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);

        Projects project = project();
        project.setDepartmentId(48L);
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setId(101L);
        assignment.setEmployeeId(12L);
        assignment.setProjectId(1L);
        assignment.setDesignationId(4L);
        assignment.setDepartmentId(48L);
        assignment.setAllocationPercentage(100);
        assignment.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        assignment.setStatus("ACTIVE");
        Employee employee = new Employee();
        employee.setId(12L);
        employee.setRitId("RIT-0012");
        employee.setFirstName("Dinakar");
        employee.setLastName("Kalaga");
        employee.setEmail("venkatd099@gmail.com");
        employee.setWorkMode("OFFSHORE");
        LookupValue designation = new LookupValue();
        designation.setId(4L);
        designation.setName("Senior Software Engineer");
        LookupValue department = new LookupValue();
        department.setId(48L);
        department.setName("Car Management");

        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(assignments.findByProjectIdAndIsCurrentTrue(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment), PageRequest.of(0, 10), 1));
        when(employees.findByIdIn(List.of(12L))).thenReturn(List.of(employee));
        when(lookups.findAllById(List.of(4L, 48L))).thenReturn(List.of(designation, department));

        var response = service.getEmployees(1L, 0, 10);

        assertEquals("CBMS", response.getProjectCode());
        assertEquals(48L, response.getDepartmentId());
        assertEquals("Car Management", response.getDepartmentName());
        assertEquals(1, response.getTotalElements());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        var result = response.getEmployees().get(0);
        assertEquals(101L, result.getAssignmentId());
        assertEquals("RIT-0012", result.getEmployeeNumber());
        assertEquals("OFFSHORE", result.getWorkMode());
        assertEquals(48L, result.getDepartmentId());
        assertEquals("Car Management", result.getDepartmentName());
        assertEquals(100, result.getAllocationPercentage());
        assertEquals(LocalDate.of(2026, 1, 1), result.getAssignmentStartDate());
    }

    @Test
    void returnsProjectMetadataForAnEmptyPage() {
        ProjectRepository projects = mock(ProjectRepository.class);
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        LookupValueRepository lookups = mock(LookupValueRepository.class);
        ProjectServiceImpl service = new ProjectServiceImpl(projects, assignments, employees, lookups);

        when(projects.findById(1L)).thenReturn(Optional.of(project()));
        when(assignments.findByProjectIdAndIsCurrentTrue(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(employees.findByIdIn(List.of())).thenReturn(List.of());
        when(lookups.findAllById(List.of())).thenReturn(List.of());

        var response = service.getEmployees(1L, 0, 10);

        assertEquals(1L, response.getProjectId());
        assertTrue(response.getEmployees().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    private Projects project() {
        Projects project = new Projects();
        project.setId(1L);
        project.setProjectCode("CBMS");
        project.setProjectName("CBMS");
        return project;
    }
}
