package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveApprovalServiceTest {
    private final CurrentEmployeeService current = mock(CurrentEmployeeService.class);
    private final LeaveRequestRepository requests = mock(LeaveRequestRepository.class);
    private final LeaveRequestApprovalRepository approvals = mock(LeaveRequestApprovalRepository.class);
    private final EmployeeLeaveBalanceRepository balances = mock(EmployeeLeaveBalanceRepository.class);
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments = mock(EmployeeLeaveBalanceAdjustmentRepository.class);
    private final LeaveApprovalService service = new LeaveApprovalService(current, requests, approvals,
            balances, adjustments);

    private LeaveRequest request(boolean withLevel2) {
        Employee employee = new Employee(); employee.setId(1L); employee.setFirstName("Worker");
        Employee level1 = new Employee(); level1.setId(2L); level1.setFirstName("Lead");
        Employee level2 = new Employee(); level2.setId(3L); level2.setFirstName("Manager");
        when(current.currentEmployee()).thenReturn(level1);
        LeaveType type = new LeaveType(); type.setId(4L); type.setName("PTO"); type.setUnit(LeaveUnit.HOURS);
        EmployeeLeavePolicy assignment = new EmployeeLeavePolicy(); assignment.setId(5L);
        LeaveRequest r = new LeaveRequest(); r.setId(6L); r.setEmployee(employee);
        r.setLevel1Approver(level1); r.setLevel2Approver(withLevel2 ? level2 : null);
        r.setLeaveType(type); r.setEmployeeLeavePolicy(assignment);
        r.setStatus(LeaveRequestStatus.SUBMITTED);
        r.setFromDate(LocalDate.of(2026, 9, 21)); r.setToDate(r.getFromDate());
        r.setTotalHours(new BigDecimal("4.00"));
        LeaveRequestDay day = new LeaveRequestDay(); day.setLeaveDate(r.getFromDate());
        day.setScheduledHours(new BigDecimal("8.00")); day.setRequestedHours(new BigDecimal("4.00"));
        r.getDays().add(day);
        when(requests.findByIdForApproval(6L)).thenReturn(Optional.of(r));
        when(requests.findById(6L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test void level1ThenLevel2AndNoDoubleApproval() {
        LeaveRequest r = request(true);
        Employee replacement = new Employee(); replacement.setId(99L);
        r.getEmployeeLeavePolicy().setLevel1Approver(replacement);
        service.approve(6L, "OK");
        assertEquals(LeaveRequestStatus.LEVEL1_APPROVED, r.getStatus());
        verify(balances, never()).save(any());
        assertThrows(InvalidOperationException.class, () -> service.approve(6L, "Again"));
        Employee level2 = r.getLevel2Approver();
        when(current.currentEmployee()).thenReturn(level2);
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(7L);
        balance.setEntitled(new BigDecimal("20.00")); balance.setOpeningBalance(BigDecimal.ZERO);
        balance.setUsed(BigDecimal.ZERO);
        when(balances.findForApproval(5L, 4L, 2026)).thenReturn(Optional.of(balance));
        service.approve(6L, "Approved");
        assertEquals(LeaveRequestStatus.APPROVED, r.getStatus());
        assertEquals(0, new BigDecimal("4.00").compareTo(balance.getUsed()));
        assertThrows(InvalidOperationException.class, () -> service.approve(6L, "Again"));
        verify(balances, times(1)).save(balance);
        verify(approvals, times(2)).saveAndFlush(any());
    }

    @Test void unauthorizedEmployeeCannotViewOrApprove() {
        request(false);
        Employee stranger = new Employee(); stranger.setId(99L);
        when(current.currentEmployee()).thenReturn(stranger);
        assertThrows(AccessDeniedException.class, () -> service.detail(6L));
        assertThrows(AccessDeniedException.class, () -> service.approve(6L, null));
        verify(approvals, never()).saveAndFlush(any());
    }

    @Test void oneLevelApprovalConsumesOnceAndRejectNeverConsumes() {
        LeaveRequest r = request(false);
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance(); balance.setId(7L);
        balance.setEntitled(new BigDecimal("20.00")); balance.setOpeningBalance(BigDecimal.ZERO);
        balance.setUsed(BigDecimal.ZERO);
        when(balances.findForApproval(5L, 4L, 2026)).thenReturn(Optional.of(balance));
        service.approve(6L, null);
        assertEquals(LeaveRequestStatus.APPROVED, r.getStatus());
        assertThrows(InvalidOperationException.class, () -> service.reject(6L, "No"));
        verify(balances, times(1)).save(balance);
    }
}
