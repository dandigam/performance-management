package com.rit.performance.service;

import com.rit.performance.dto.response.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveApprovalService {
    private final CurrentEmployeeService currentEmployee;
    private final LeaveRequestRepository requests;
    private final LeaveRequestApprovalRepository approvals;
    private final EmployeeLeaveBalanceRepository balances;
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments;

    public List<TeamLeaveRequestResponse> team(String view) {
        Long approverId = currentEmployee.currentEmployee().getId();
        return requests.findAllByOrderBySubmittedAtDescIdDesc().stream()
                .filter(r -> r.getStatus() != LeaveRequestStatus.DRAFT)
                .filter(r -> level(r, approverId) != null)
                .filter(r -> switch (view) {
                    case "pending" -> actionable(r, level(r, approverId));
                    case "approved" -> r.getStatus() == LeaveRequestStatus.APPROVED;
                    case "rejected" -> r.getStatus() == LeaveRequestStatus.REJECTED;
                    default -> true;
                })
                .map(r -> summary(r, level(r, approverId))).toList();
    }

    public TeamLeaveDetailResponse detail(Long id) {
        LeaveRequest request = requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        LeaveApprovalLevel level = requireLevel(request, currentEmployee.currentEmployee().getId());
        Map<Integer, Object> availableByYear = new TreeMap<>();
        request.getDays().forEach(d -> availableByYear.computeIfAbsent(d.getLeaveDate().getYear(),
                year -> availability(request, year)));
        Employee employee = request.getEmployee();
        return new TeamLeaveDetailResponse(request.getId(), employee.getId(), name(employee), employee.getEmail(),
                request.getLeaveType().getId(), request.getLeaveType().getName(), request.getLeaveType().getUnit(),
                request.getFromDate(), request.getToDate(), request.getTotalHours(),
                availableByYear.get(request.getFromDate().getYear()), availableByYear,
                request.getReason(), request.getNotes(), request.getSubmittedAt(), request.getStatus(), level,
                request.getDays().stream().map(d -> new LeaveRequestDayResponse(d.getId(), d.getLeaveDate(),
                        d.getScheduledHours(), d.getRequestedHours())).toList(),
                approvals.findByLeaveRequestIdOrderByActionAtAscIdAsc(id).stream()
                        .map(a -> new LeaveApprovalHistoryResponse(a.getId(), a.getApprovalLevel(),
                                a.getApprover().getId(), name(a.getApprover()), a.getAction(),
                                a.getComments(), a.getActionAt())).toList());
    }

    @Transactional
    public TeamLeaveDetailResponse approve(Long id, String comments) {
        return act(id, LeaveApprovalAction.APPROVED, comments);
    }

    @Transactional
    public TeamLeaveDetailResponse reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) throw new InvalidOperationException("Rejection reason is required.");
        return act(id, LeaveApprovalAction.REJECTED, reason.trim());
    }

    private TeamLeaveDetailResponse act(Long id, LeaveApprovalAction action, String comments) {
        Employee approver = currentEmployee.currentEmployee();
        LeaveRequest request = requests.findByIdForApproval(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        LeaveApprovalLevel level = requireLevel(request, approver.getId());
        if (!actionable(request, level))
            throw new InvalidOperationException("This leave request is not awaiting your approval.");
        boolean finalApproval = action == LeaveApprovalAction.APPROVED
                && (level == LeaveApprovalLevel.LEVEL2 || request.getLevel2Approver() == null);
        if (finalApproval) consumeBalance(request);
        LeaveRequestApproval history = new LeaveRequestApproval();
        history.setLeaveRequest(request);
        history.setApprovalLevel(level);
        history.setApprover(approver);
        history.setAction(action);
        history.setComments(comments);
        history.setActionAt(LocalDateTime.now());
        if (action == LeaveApprovalAction.REJECTED) request.setStatus(LeaveRequestStatus.REJECTED);
        else if (finalApproval) request.setStatus(LeaveRequestStatus.APPROVED);
        else request.setStatus(LeaveRequestStatus.LEVEL1_APPROVED);
        requests.saveAndFlush(request);
        approvals.saveAndFlush(history);
        return detail(id);
    }

    private void consumeBalance(LeaveRequest request) {
        Map<Integer, List<LeaveRequestDay>> byYear = request.getDays().stream()
                .collect(Collectors.groupingBy(d -> d.getLeaveDate().getYear(), TreeMap::new, Collectors.toList()));
        for (var entry : byYear.entrySet()) {
            int year = entry.getKey();
            EmployeeLeaveBalance balance = balances.findForApproval(request.getEmployeeLeavePolicy().getId(),
                    request.getLeaveType().getId(), year).orElseThrow(() -> new InvalidOperationException(
                            "Initialize the leave balance for year " + year + " first."));
            if (balance.getStatus() != LeavePolicyStatus.ACTIVE)
                throw new InvalidOperationException("Leave balance is inactive for year " + year + ".");
            if (balance.getEntitled() == null) continue;
            BigDecimal amount = entry.getValue().stream().map(d -> request.getLeaveType().getUnit() == LeaveUnit.HOURS
                    ? d.getRequestedHours()
                    : d.getRequestedHours().divide(d.getScheduledHours(), 6, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (amount.compareTo(available(balance)) > 0)
                throw new InvalidOperationException("Requested leave exceeds current available balance for year " + year + ".");
            balance.setUsed(balance.getUsed().add(amount));
            balances.save(balance);
        }
    }

    private TeamLeaveRequestResponse summary(LeaveRequest r, LeaveApprovalLevel level) {
        return new TeamLeaveRequestResponse(r.getId(), r.getEmployee().getId(), name(r.getEmployee()),
                r.getLeaveType().getName(), r.getFromDate(), r.getToDate(), r.getTotalHours(), r.getSubmittedAt(),
                availability(r, r.getFromDate().getYear()), level, r.getStatus());
    }

    private Object availability(LeaveRequest r, int year) {
        return balances.findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(
                        r.getEmployeeLeavePolicy().getId(), r.getLeaveType().getId(), year)
                .map(b -> b.getEntitled() == null ? "Unlimited" : available(b)).orElse(null);
    }

    private BigDecimal available(EmployeeLeaveBalance balance) {
        BigDecimal total = adjustments.findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(balance.getId())
                .stream().map(a -> a.getAdjustmentType() == LeaveBalanceAdjustmentType.ADD
                        ? a.getAmount() : a.getAmount().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.getOpeningBalance().add(balance.getEntitled()).add(total).subtract(balance.getUsed());
    }

    private LeaveApprovalLevel requireLevel(LeaveRequest request, Long approverId) {
        LeaveApprovalLevel level = level(request, approverId);
        if (level == null) throw new AccessDeniedException("You are not configured to approve this leave request.");
        return level;
    }

    private LeaveApprovalLevel level(LeaveRequest request, Long approverId) {
        if (request.getLevel1Approver() != null && request.getLevel1Approver().getId().equals(approverId))
            return LeaveApprovalLevel.LEVEL1;
        if (request.getLevel2Approver() != null && request.getLevel2Approver().getId().equals(approverId))
            return LeaveApprovalLevel.LEVEL2;
        return null;
    }

    private boolean actionable(LeaveRequest r, LeaveApprovalLevel level) {
        return level == LeaveApprovalLevel.LEVEL1 && r.getStatus() == LeaveRequestStatus.SUBMITTED
                || level == LeaveApprovalLevel.LEVEL2 && r.getStatus() == LeaveRequestStatus.LEVEL1_APPROVED;
    }

    private String name(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }
}
