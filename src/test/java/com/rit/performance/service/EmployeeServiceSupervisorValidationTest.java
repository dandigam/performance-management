package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.dto.EmployeeUpdateRequest;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.ProjectRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceSupervisorValidationTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeAssignmentRepository assignmentRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeRoleRepository employeeRoleRepository;
    @Mock private EmployeeReviewAssessmentRepository employeeReviewAssessmentRepository;
    @Mock private EmployeeReviewRepository employeeReviewRepository;
    @Mock private PerformanceCycleConfigRepository cycleRepository;
    @Mock private VendorRepository vendorRepository;

    @Test
    void acceptsAnyActiveEmployeeAsSupervisor() {
        Employee supervisor = new Employee();
        supervisor.setId(2L);
        supervisor.setStatus("ACTIVE");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(supervisor));
        EmployeeServiceImpl service = new EmployeeServiceImpl(employeeRepository, assignmentRepository,
                lookupValueRepository, projectRepository, userRepository, employeeRoleRepository,
                employeeReviewAssessmentRepository, employeeReviewRepository, cycleRepository, vendorRepository);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "validateSupervisor", 1L, 2L, "Team Lead"));
    }

    @Test
    void employeeUpdateReactivatesTheSameProjectAssignment() {
        EmployeeAssignment inactive = new EmployeeAssignment();
        inactive.setId(88L);
        inactive.setEmployeeId(2L);
        inactive.setProjectId(1L);
        inactive.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        inactive.setEffectiveTo(LocalDate.of(2026, 8, 1));
        inactive.setStatus("INACTIVE");
        inactive.setIsCurrent(false);
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setProjectId(1L);
        request.setAssignmentEffectiveFrom(LocalDate.of(2026, 8, 9));
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.save(inactive)).thenReturn(inactive);
        EmployeeServiceImpl service = new EmployeeServiceImpl(employeeRepository, assignmentRepository,
                lookupValueRepository, projectRepository, userRepository, employeeRoleRepository,
                employeeReviewAssessmentRepository, employeeReviewRepository, cycleRepository, vendorRepository);

        EmployeeAssignment result = ReflectionTestUtils.invokeMethod(service,
                "replaceAssignmentWhenChanged", 2L, inactive, request, true);

        assertSame(inactive, result);
        assertEquals(88L, result.getId());
        assertTrue(result.getIsCurrent());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(LocalDate.of(2026, 8, 9), result.getEffectiveFrom());
        assertNull(result.getEffectiveTo());
    }
}
