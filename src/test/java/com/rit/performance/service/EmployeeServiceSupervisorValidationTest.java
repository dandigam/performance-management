package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.ProjectRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void acceptsAnyActiveEmployeeAsSupervisor() {
        Employee supervisor = new Employee();
        supervisor.setId(2L);
        supervisor.setStatus("ACTIVE");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(supervisor));
        EmployeeServiceImpl service = new EmployeeServiceImpl(employeeRepository, assignmentRepository,
                lookupValueRepository, projectRepository, userRepository, employeeRoleRepository,
                employeeReviewAssessmentRepository, employeeReviewRepository, cycleRepository);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "validateSupervisor", 1L, 2L, "Team Lead"));
    }
}
