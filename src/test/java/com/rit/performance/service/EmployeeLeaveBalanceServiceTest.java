package com.rit.performance.service;

import com.rit.performance.dto.request.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeLeaveBalanceServiceTest {
    private final EmployeeLeaveBalanceRepository balances = mock(EmployeeLeaveBalanceRepository.class);
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments = mock(EmployeeLeaveBalanceAdjustmentRepository.class);
    private final EmployeeLeavePolicyRepository assignments = mock(EmployeeLeavePolicyRepository.class);
    private final LeavePolicyRuleRepository rules = mock(LeavePolicyRuleRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeLeaveBalanceService service = new EmployeeLeaveBalanceService(
            balances, adjustments, assignments, rules, employees);

    private EmployeeLeavePolicy assignment() {
        Employee employee = new Employee(); employee.setId(1L);
        LeavePolicy policy = new LeavePolicy(); policy.setId(2L);
        EmployeeLeavePolicy assignment = new EmployeeLeavePolicy(); assignment.setId(3L);
        assignment.setEmployee(employee); assignment.setLeavePolicy(policy);
        assignment.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        assignment.setEffectiveTo(LocalDate.of(2026, 12, 31));
        return assignment;
    }

    private LeavePolicyRule rule(Long typeId, BigDecimal entitlement) {
        LeaveType type = new LeaveType(); type.setId(typeId); type.setCode("TYPE" + typeId);
        type.setName("Leave " + typeId); type.setUnit(LeaveUnit.HOURS);
        LeavePolicyRule rule = new LeavePolicyRule(); rule.setLeaveType(type); rule.setEntitlement(entitlement);
        return rule;
    }

    @Test void initializesActiveRulesOnceAndPreservesExistingBalances() {
        EmployeeLeavePolicy assignment = assignment();
        when(employees.findByIdForLeavePolicyUpdate(1L)).thenReturn(Optional.of(assignment.getEmployee()));
        when(assignments.findById(3L)).thenReturn(Optional.of(assignment));
        LeavePolicyRule active = rule(4L, new BigDecimal("120.00"));
        LeavePolicyRule unlimited = rule(5L, null);
        LeavePolicyRule inactive = rule(6L, BigDecimal.TEN); inactive.setStatus(LeavePolicyStatus.INACTIVE);
        when(rules.findByLeavePolicyIdOrderByIdAsc(2L)).thenReturn(List.of(active, unlimited, inactive));
        when(balances.existsByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(3L, 4L, 2026)).thenReturn(true);
        when(balances.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.initialize(1L, new InitializeLeaveBalancesRequest(3L, 2026));
        verify(balances, times(1)).saveAndFlush(argThat(b -> b.getLeaveType().getId().equals(5L)
                && b.getEntitled() == null && b.getOpeningBalance().compareTo(BigDecimal.ZERO) == 0));
        verify(balances, never()).saveAndFlush(argThat(b -> b.getLeaveType().getId().equals(6L)));
    }

    @Test void calculatesSignedAdjustmentsAndUnlimitedAvailability() {
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(7L);
        balance.setEmployee(assignment().getEmployee()); balance.setEmployeeLeavePolicy(assignment());
        balance.setLeaveType(rule(4L, BigDecimal.TEN).getLeaveType());
        balance.setOpeningBalance(new BigDecimal("5.00"));
        balance.setEntitled(new BigDecimal("20.00")); balance.setUsed(new BigDecimal("4.00"));
        when(balances.findById(7L)).thenReturn(Optional.of(balance));
        EmployeeLeaveBalanceAdjustment add = new EmployeeLeaveBalanceAdjustment();
        add.setAdjustmentType(LeaveBalanceAdjustmentType.ADD); add.setAmount(new BigDecimal("3.00"));
        EmployeeLeaveBalanceAdjustment deduct = new EmployeeLeaveBalanceAdjustment();
        deduct.setAdjustmentType(LeaveBalanceAdjustmentType.DEDUCT); deduct.setAmount(new BigDecimal("2.00"));
        when(adjustments.findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(7L)).thenReturn(List.of(add, deduct));
        var response = service.getById(1L, 7L);
        assertEquals(0, response.totalAdjustments().compareTo(BigDecimal.ONE));
        assertEquals(0, ((BigDecimal) response.available()).compareTo(new BigDecimal("22.00")));
        balance.setEntitled(null);
        response = service.getById(1L, 7L);
        assertTrue(response.unlimited()); assertEquals("Unlimited", response.available());
    }

    @Test void rejectsInactiveAssignmentAndYearOutsidePeriod() {
        EmployeeLeavePolicy assignment = assignment();
        when(employees.findByIdForLeavePolicyUpdate(1L)).thenReturn(Optional.of(assignment.getEmployee()));
        when(assignments.findById(3L)).thenReturn(Optional.of(assignment));
        assertThrows(InvalidOperationException.class,
                () -> service.initialize(1L, new InitializeLeaveBalancesRequest(3L, 2027)));
        assignment.setStatus(LeavePolicyStatus.INACTIVE);
        assertThrows(InvalidOperationException.class,
                () -> service.initialize(1L, new InitializeLeaveBalancesRequest(3L, 2026)));
    }

    @Test void adjustmentCreatesHistoryWithoutChangingBalance() {
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(7L);
        balance.setEmployee(assignment().getEmployee());
        when(balances.findById(7L)).thenReturn(Optional.of(balance));
        when(adjustments.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.addAdjustment(1L, 7L, new LeaveBalanceAdjustmentRequest(
                LeaveBalanceAdjustmentType.ADD, BigDecimal.TEN, " Correction ", null, LocalDate.of(2026, 3, 1)));
        verify(adjustments).saveAndFlush(argThat(a -> a.getReason().equals("Correction")
                && a.getAmount().equals(BigDecimal.TEN)));
        verify(balances, never()).save(any());
    }
}
