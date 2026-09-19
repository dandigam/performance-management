package com.rit.performance.service;

import com.rit.performance.dto.request.EmployeeLeavePolicyRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeLeavePolicyServiceTest {
    private final EmployeeLeavePolicyRepository assignments = mock(EmployeeLeavePolicyRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final LeavePolicyRepository policies = mock(LeavePolicyRepository.class);
    private final EmployeeLeavePolicyService service = new EmployeeLeavePolicyService(assignments, employees, policies);
    private final LocalDate start = LocalDate.of(2026, 1, 1);

    private LeavePolicy policy(LocalDate end) {
        LeavePolicy policy = new LeavePolicy(); policy.setId(2L); policy.setPolicyName("India full time");
        policy.setEffectiveFrom(start); policy.setEffectiveTo(end);
        return policy;
    }

    private EmployeeLeavePolicyRequest request(LocalDate from, LocalDate to) {
        return new EmployeeLeavePolicyRequest(2L, from, to);
    }

    private void stubEmployeeAndPolicy(LeavePolicy policy) {
        Employee employee = new Employee(); employee.setId(1L);
        when(employees.findByIdForLeavePolicyUpdate(1L)).thenReturn(Optional.of(employee));
        when(policies.findById(2L)).thenReturn(Optional.of(policy));
        when(assignments.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void assignsWithinPolicyPeriodAndAllowsAdjacentPeriods() {
        stubEmployeeAndPolicy(policy(LocalDate.of(2026, 12, 31)));
        var result = service.assign(1L, request(start, LocalDate.of(2026, 6, 30)));
        assertEquals(1L, result.employeeId());
        assertEquals(LeavePolicyStatus.ACTIVE, result.status());
        verify(assignments).countOverlaps(1L, LeavePolicyStatus.ACTIVE, -1L, start, LocalDate.of(2026, 6, 30));
        service.assign(1L, request(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)));
    }

    @Test void rejectsInvalidPeriodInactivePolicyAndOverlaps() {
        LeavePolicy policy = policy(LocalDate.of(2026, 12, 31));
        stubEmployeeAndPolicy(policy);
        assertThrows(InvalidOperationException.class,
                () -> service.assign(1L, request(start.plusDays(2), start.plusDays(1))));
        assertThrows(InvalidOperationException.class, () -> service.assign(1L, request(start, null)));
        assertThrows(InvalidOperationException.class,
                () -> service.assign(1L, request(start.minusDays(1), start)));
        when(assignments.countOverlaps(any(), any(), any(), any(), any())).thenReturn(1L);
        assertThrows(InvalidOperationException.class,
                () -> service.assign(1L, request(start, LocalDate.of(2026, 6, 30))));
        policy.setStatus(LeavePolicyStatus.INACTIVE);
        assertThrows(InvalidOperationException.class,
                () -> service.assign(1L, request(start, LocalDate.of(2026, 6, 30))));
    }

    @Test void activationChecksOverlapAndHistoryIsRetained() {
        LeavePolicy policy = policy(null);
        stubEmployeeAndPolicy(policy);
        EmployeeLeavePolicy assignment = new EmployeeLeavePolicy(); assignment.setId(3L);
        assignment.setEmployee(employees.findByIdForLeavePolicyUpdate(1L).orElseThrow());
        assignment.setLeavePolicy(policy); assignment.setEffectiveFrom(start);
        assignment.setStatus(LeavePolicyStatus.INACTIVE);
        when(assignments.findById(3L)).thenReturn(Optional.of(assignment));
        when(assignments.countOverlaps(any(), any(), any(), any(), any())).thenReturn(1L);
        assertThrows(InvalidOperationException.class,
                () -> service.changeStatus(1L, 3L, LeavePolicyStatus.ACTIVE));
        assertEquals(LeavePolicyStatus.INACTIVE, assignment.getStatus());
        service.changeStatus(1L, 3L, LeavePolicyStatus.INACTIVE);
        verify(assignments).saveAndFlush(assignment);
        verify(assignments, never()).delete(any());
    }

    @Test void currentPolicyUsesTodayAndMissingAssignmentsReturn404() {
        Employee employee = new Employee(); employee.setId(1L);
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        assertThrows(ResourceNotFoundException.class, () -> service.getCurrent(1L));
        verify(assignments).findCurrent(1L, LeavePolicyStatus.ACTIVE, LocalDate.now());
        when(assignments.findByEmployeeIdOrderByEffectiveFromDescIdDesc(1L)).thenReturn(List.of());
        assertTrue(service.getForEmployee(1L).isEmpty());
    }
}
