package com.rit.performance.service;

import com.rit.performance.dto.request.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MyLeaveServiceTest {
    private final CurrentEmployeeService current = mock(CurrentEmployeeService.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeLeavePolicyRepository assignments = mock(EmployeeLeavePolicyRepository.class);
    private final LeavePolicyRuleRepository rules = mock(LeavePolicyRuleRepository.class);
    private final TimesheetEmployeeProjectDayRepository schedules = mock(TimesheetEmployeeProjectDayRepository.class);
    private final EmployeeLeaveBalanceRepository balances = mock(EmployeeLeaveBalanceRepository.class);
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments = mock(EmployeeLeaveBalanceAdjustmentRepository.class);
    private final EmployeeLeaveBalanceService balanceService = mock(EmployeeLeaveBalanceService.class);
    private final LeaveRequestRepository requests = mock(LeaveRequestRepository.class);
    private final MyLeaveService service = new MyLeaveService(current, employees, assignments, rules,
            schedules, balances, adjustments, balanceService, requests);
    private final LocalDate date = LocalDate.of(2026, 9, 21);

    private LeaveRequestDraftRequest input(BigDecimal hours) {
        return new LeaveRequestDraftRequest(4L, date, date, "Medical appointment", null,
                List.of(new LeaveRequestDayRequest(date, hours)));
    }

    private void setup(LeaveUnit unit, BigDecimal entitlement, BigDecimal scheduledHours) {
        Employee employee = new Employee(); employee.setId(1L);
        when(current.currentEmployee()).thenReturn(employee);
        LeavePolicy policy = new LeavePolicy(); policy.setId(2L);
        EmployeeLeavePolicy assignment = new EmployeeLeavePolicy(); assignment.setId(3L);
        assignment.setEmployee(employee); assignment.setLeavePolicy(policy);
        Employee approver = new Employee(); approver.setId(40L);
        assignment.setLevel1Approver(approver);
        when(assignments.findCovering(1L, date, date, LeavePolicyStatus.ACTIVE)).thenReturn(List.of(assignment));
        LeaveType type = new LeaveType(); type.setId(4L); type.setCode("PTO");
        type.setName("Paid time off"); type.setUnit(unit);
        LeavePolicyRule rule = new LeavePolicyRule(); rule.setLeaveType(type); rule.setEntitlement(entitlement);
        when(rules.findByLeavePolicyIdAndLeaveTypeIdAndStatus(2L, 4L, LeavePolicyStatus.ACTIVE))
                .thenReturn(Optional.of(rule));
        TimesheetEmployeeProjectDay day = new TimesheetEmployeeProjectDay();
        day.setWorkDate(date); day.setScheduledHours(scheduledHours);
        when(schedules.findLeaveSchedule(eq(1L), eq(date), eq(date), any(), any()))
                .thenReturn(List.of(day));
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(5L);
        balance.setEntitled(entitlement); balance.setOpeningBalance(BigDecimal.ZERO);
        balance.setUsed(BigDecimal.ZERO);
        when(balances.findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(3L, 4L, 2026))
                .thenReturn(Optional.of(balance));
        when(requests.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void draftTotalUsesRequestedHoursAndSubmittedRequestCannotBeEdited() {
        setup(LeaveUnit.HOURS, new BigDecimal("20.00"), new BigDecimal("8.00"));
        assertEquals(new BigDecimal("4.00"), service.createDraft(input(new BigDecimal("4.00"))).totalHours());
        LeaveRequest persisted = invocationRequest();
        persisted.setId(6L);
        assertNull(persisted.getLevel1Approver());
        Employee newApprover = new Employee(); newApprover.setId(41L);
        persisted.getEmployeeLeavePolicy().setLevel1Approver(newApprover);
        when(requests.findByIdAndEmployeeId(6L, 1L)).thenReturn(Optional.of(persisted));
        when(requests.findByIdForApproval(6L)).thenReturn(Optional.of(persisted));
        when(employees.findByIdForLeavePolicyUpdate(1L)).thenReturn(Optional.of(persisted.getEmployee()));
        var submitted = service.submit(6L);
        assertEquals(LeaveRequestStatus.SUBMITTED, submitted.status());
        assertEquals(41L, persisted.getLevel1Approver().getId());
        assertNotNull(submitted.submittedAt());
        assertEquals(new BigDecimal("4.00"), submitted.totalHours());
        verify(balances, never()).save(any());
        assertThrows(InvalidOperationException.class, () -> service.updateDraft(6L, input(BigDecimal.ONE)));
        assertEquals(LeaveRequestStatus.CANCELLED, service.cancel(6L).status());
    }

    @Test void rejectsMoreThanScheduledAndBlockingDates() {
        setup(LeaveUnit.HOURS, new BigDecimal("20.00"), new BigDecimal("4.00"));
        assertThrows(InvalidOperationException.class,
                () -> service.createDraft(input(new BigDecimal("5.00"))));
        when(requests.findBlockingRequests(eq(1L), eq(-1L), any(), any())).thenReturn(List.of(new LeaveRequest()));
        assertThrows(InvalidOperationException.class,
                () -> service.createDraft(input(new BigDecimal("2.00"))));
    }

    @Test void dayUnitUsesFractionOfScheduledDayAndUnlimitedSkipsNumericCheck() {
        setup(LeaveUnit.DAYS, new BigDecimal("0.50"), new BigDecimal("4.00"));
        assertEquals(new BigDecimal("2.00"), service.createDraft(input(new BigDecimal("2.00"))).totalHours());
        assertThrows(InvalidOperationException.class,
                () -> service.createDraft(input(new BigDecimal("4.00"))));
        when(balances.findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(3L, 4L, 2026))
                .thenReturn(Optional.of(unlimitedBalance()));
        assertEquals(new BigDecimal("4.00"), service.createDraft(input(new BigDecimal("4.00"))).totalHours());
    }

    @Test void onlyFindsAuthenticatedEmployeesOwnRequest() {
        Employee employee = new Employee(); employee.setId(1L);
        when(current.currentEmployee()).thenReturn(employee);
        assertThrows(ResourceNotFoundException.class, () -> service.myRequest(99L));
        verify(requests).findByIdAndEmployeeId(99L, 1L);
    }

    private EmployeeLeaveBalance unlimitedBalance() {
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(5L);
        balance.setEntitled(null); balance.setOpeningBalance(BigDecimal.ZERO); balance.setUsed(BigDecimal.ZERO);
        return balance;
    }

    private LeaveRequest invocationRequest() {
        var captor = org.mockito.ArgumentCaptor.forClass(LeaveRequest.class);
        verify(requests).saveAndFlush(captor.capture());
        return captor.getValue();
    }

}
