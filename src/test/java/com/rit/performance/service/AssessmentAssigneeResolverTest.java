package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentAssigneeResolverTest {

    @Test
    void resolvesLeadAndManagerDirectlyFromCurrentAssignment() {
        EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AssessmentAssigneeResolver resolver = new AssessmentAssigneeResolver(assignments, employees,
                mock(EmployeeRoleRepository.class), mock(LookupValueRepository.class), mock(UserRepository.class));

        Employee subject = employee(10L);
        Employee lead = employee(20L);
        Employee manager = employee(30L);
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployeeId(subject.getId());
        assignment.setLeadId(lead.getId());
        assignment.setManagerId(manager.getId());
        assignment.setIsCurrent(true);

        when(assignments.findByEmployeeIdAndIsCurrentTrue(subject.getId())).thenReturn(Optional.of(assignment));
        when(employees.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(employees.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertSame(lead, resolver.resolve(subject, role("LEAD", "Team Lead")));
        assertSame(manager, resolver.resolve(subject, role("MANAGER", "Manager")));
    }

    private static Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }

    private static LookupValue role(String code, String name) {
        LookupValue role = new LookupValue();
        role.setCode(code);
        role.setName(name);
        return role;
    }
}
