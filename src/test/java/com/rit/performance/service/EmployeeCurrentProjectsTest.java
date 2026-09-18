package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeCurrentProjectsTest {
    @Mock EmployeeRepository employeeRepository;
    @Mock EmployeeAssignmentRepository assignmentRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock SowRepository sowRepository;
    @Mock SowMilestoneRepository sowMilestoneRepository;
    @Mock SowMilestonePositionAssignmentRepository milestonePositionAssignmentRepository;
    @Mock EmployeeRoleRepository employeeRoleRepository;
    @Mock EmployeeAddressRepository employeeAddressRepository;
    @Mock EmployeeCompensationRepository employeeCompensationRepository;
    @Mock EmployeeProfessionalProfileRepository employeeProfessionalProfileRepository;
    @Mock EmployeeEducationRepository employeeEducationRepository;
    @Mock EmployeeExperienceRepository employeeExperienceRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @InjectMocks EmployeeServiceImpl service;

    @Test void detailsIncludeCurrentAssignedProjectAndPreserveHistory() {
        var employee = new Employee(); employee.setId(2L); employee.setFirstName("Charan");
        employee.setStatus("ACTIVE");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        var now = LocalDate.now();
        var assigned = assignment(1L, "ASSIGNED", now.minusDays(10), null);
        var completed = assignment(2L, "COMPLETED", now.minusMonths(2), now.minusMonths(1));
        var future = assignment(3L, "ASSIGNED", now.plusDays(1), null);
        var expired = assignment(4L, "ASSIGNED", now.minusDays(10), now.minusDays(1));
        var legacy = assignment(5L, "ACTIVE", now.minusDays(10), null);
        var assignments = List.of(assigned, completed, future, expired, legacy);
        when(assignmentRepository.findByEmployeeId(2L)).thenReturn(assignments);
        when(sowRepository.findAll()).thenReturn(assignments.stream().<Sow>map(a -> Sow.builder()
                .id(a.getSowId()).sowName("Project " + a.getSowId()).build()).toList());

        var result = service.getById(2L);

        assertThat(result.getCurrentProjects()).hasSize(1);
        assertThat(result.getCurrentProjects().get(0).getSowId()).isEqualTo(1L);
        assertThat(result.getCurrentProjects().get(0).getSowName()).isEqualTo("Project 1");
        assertThat(result.getAssignmentList()).hasSize(5);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    private EmployeeAssignment assignment(Long sowId, String status, LocalDate start, LocalDate end) {
        var assignment = new EmployeeAssignment(); assignment.setId(sowId); assignment.setEmployeeId(2L);
        assignment.setSowId(sowId); assignment.setStatus(status);
        assignment.setEffectiveFrom(start); assignment.setEffectiveTo(end); return assignment;
    }
}
