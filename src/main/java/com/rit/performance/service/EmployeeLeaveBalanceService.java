package com.rit.performance.service;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLeaveBalanceService {
    private final EmployeeLeaveBalanceRepository balances;
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments;
    private final EmployeeLeavePolicyRepository assignments;
    private final LeavePolicyRuleRepository rules;
    private final EmployeeRepository employees;

    @Transactional
    public List<EmployeeLeaveBalanceResponse> initialize(Long employeeId, InitializeLeaveBalancesRequest request) {
        employees.findByIdForLeavePolicyUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        EmployeeLeavePolicy assignment = assignments.findById(request.employeeLeavePolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy assignment not found: " + request.employeeLeavePolicyId()));
        if (!assignment.getEmployee().getId().equals(employeeId))
            throw new ResourceNotFoundException("Leave policy assignment not found: " + request.employeeLeavePolicyId());
        if (assignment.getStatus() != LeavePolicyStatus.ACTIVE)
            throw new InvalidOperationException("Leave policy assignment must be active.");
        LocalDate yearStart = LocalDate.of(request.balanceYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(request.balanceYear(), 12, 31);
        if (assignment.getEffectiveFrom().isAfter(yearEnd)
                || (assignment.getEffectiveTo() != null && assignment.getEffectiveTo().isBefore(yearStart)))
            throw new InvalidOperationException("Balance year must overlap the assignment period.");

        for (LeavePolicyRule rule : rules.findByLeavePolicyIdOrderByIdAsc(assignment.getLeavePolicy().getId())) {
            if (rule.getStatus() != LeavePolicyStatus.ACTIVE
                    || balances.existsByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(
                            assignment.getId(), rule.getLeaveType().getId(), request.balanceYear())) continue;
            EmployeeLeaveBalance balance = new EmployeeLeaveBalance();
            balance.setEmployee(assignment.getEmployee());
            balance.setEmployeeLeavePolicy(assignment);
            balance.setLeaveType(rule.getLeaveType());
            balance.setBalanceYear(request.balanceYear());
            balance.setEntitled(rule.getEntitlement());
            balances.saveAndFlush(balance);
        }
        return balances.findByEmployeeLeavePolicyIdAndBalanceYearOrderByIdAsc(
                assignment.getId(), request.balanceYear()).stream().map(this::response).toList();
    }

    public List<EmployeeLeaveBalanceResponse> getForEmployee(Long employeeId, int year) {
        requireEmployee(employeeId);
        return balances.findByEmployeeIdAndBalanceYearOrderByIdAsc(employeeId, year).stream()
                .map(this::response).toList();
    }

    public EmployeeLeaveBalanceResponse getById(Long employeeId, Long balanceId) {
        return response(findBalance(employeeId, balanceId));
    }

    @Transactional
    public LeaveBalanceAdjustmentResponse addAdjustment(Long employeeId, Long balanceId,
            LeaveBalanceAdjustmentRequest request) {
        EmployeeLeaveBalance balance = findBalance(employeeId, balanceId);
        EmployeeLeaveBalanceAdjustment adjustment = new EmployeeLeaveBalanceAdjustment();
        adjustment.setEmployeeLeaveBalance(balance);
        adjustment.setAdjustmentType(request.adjustmentType());
        adjustment.setAmount(request.amount());
        adjustment.setReason(request.reason().trim());
        adjustment.setNotes(request.notes());
        adjustment.setAdjustmentDate(request.adjustmentDate());
        return adjustmentResponse(adjustments.saveAndFlush(adjustment));
    }

    public List<LeaveBalanceAdjustmentResponse> getAdjustments(Long employeeId, Long balanceId) {
        findBalance(employeeId, balanceId);
        return adjustments.findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(balanceId).stream()
                .map(this::adjustmentResponse).toList();
    }

    private void requireEmployee(Long employeeId) {
        if (!employees.existsById(employeeId)) throw new ResourceNotFoundException("Employee not found: " + employeeId);
    }

    private EmployeeLeaveBalance findBalance(Long employeeId, Long balanceId) {
        EmployeeLeaveBalance balance = balances.findById(balanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee leave balance not found: " + balanceId));
        if (!balance.getEmployee().getId().equals(employeeId))
            throw new ResourceNotFoundException("Employee leave balance not found: " + balanceId);
        return balance;
    }

    private EmployeeLeaveBalanceResponse response(EmployeeLeaveBalance balance) {
        BigDecimal total = adjustments.findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(balance.getId())
                .stream().map(a -> a.getAdjustmentType() == LeaveBalanceAdjustmentType.ADD
                        ? a.getAmount() : a.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean unlimited = balance.getEntitled() == null;
        Object available = unlimited ? "Unlimited" : balance.getOpeningBalance().add(balance.getEntitled())
                .add(total).subtract(balance.getUsed());
        LeaveType type = balance.getLeaveType();
        return new EmployeeLeaveBalanceResponse(balance.getId(), balance.getEmployee().getId(),
                balance.getEmployeeLeavePolicy().getId(), type.getId(), type.getCode(), type.getName(),
                type.getUnit(), balance.getBalanceYear(), balance.getOpeningBalance(), balance.getEntitled(),
                total, balance.getUsed(), available, unlimited, balance.getStatus(), balance.getCreatedOn(),
                balance.getCreatedBy(), balance.getUpdatedOn(), balance.getUpdatedBy());
    }

    private LeaveBalanceAdjustmentResponse adjustmentResponse(EmployeeLeaveBalanceAdjustment adjustment) {
        return new LeaveBalanceAdjustmentResponse(adjustment.getId(), adjustment.getEmployeeLeaveBalance().getId(),
                adjustment.getAdjustmentType(), adjustment.getAmount(), adjustment.getReason(), adjustment.getNotes(),
                adjustment.getAdjustmentDate(), adjustment.getCreatedAt(), adjustment.getCreatedBy());
    }
}
