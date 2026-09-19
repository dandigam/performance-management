package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetApprovalRequest;
import com.rit.performance.dto.response.TimesheetApprovalUpdateResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.TimesheetRepository;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TimesheetApprovalService {
    private final TimesheetRepository timesheets;
    private final UserRepository users;
    private final Clock clock;

    @Transactional
    public TimesheetApprovalUpdateResponse update(Long timesheetId, Long approvalId,
            TimesheetApprovalRequest request, String username) {
        if (username == null) throw new AuthenticationException("Authentication is required");
        var user = users.findByUsernameIgnoreCase(username)
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new AuthenticationException("User account is not available"));
        if (request == null || (request.status() != TimesheetApprovalStatus.APPROVED
                && request.status() != TimesheetApprovalStatus.REJECTED))
            throw new InvalidOperationException("status must be APPROVED or REJECTED");
        if (request.comments() != null && request.comments().length() > 2000)
            throw new InvalidOperationException("comments must not exceed 2000 characters");
        // Serialize decisions for both approval levels using the timesheet row lock.
        var sheet = timesheets.findForEntryUpdate(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found: " + timesheetId));
        var approval = sheet.getApprovals().stream()
                .filter(a -> Objects.equals(a.getId(), approvalId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found for timesheet: " + approvalId));
        if (user.getEmployee() == null || !Objects.equals(user.getEmployee().getId(),
                approval.getApproverEmployee().getId()))
            throw new AccessDeniedException("Only the assigned approver can action this approval");
        if (approval.getStatus() != TimesheetApprovalStatus.PENDING)
            throw new InvalidOperationException("This approval has already been actioned");
        boolean primary = Integer.valueOf(1).equals(approval.getApprovalLevel());
        boolean secondary = Integer.valueOf(2).equals(approval.getApprovalLevel());
        if ((!primary && !secondary)
                || (primary && sheet.getStatus() != TimesheetStatus.SUBMITTED)
                || (secondary && (sheet.getStatus() != TimesheetStatus.LEVEL1_APPROVED
                    || sheet.getApprovals().stream().noneMatch(a -> Integer.valueOf(1).equals(a.getApprovalLevel())
                        && a.getStatus() == TimesheetApprovalStatus.APPROVED))))
            throw new InvalidOperationException("Approval is not available at the current timesheet stage");
        approval.setStatus(request.status());
        approval.setComments(request.comments());
        approval.setActionAt(LocalDateTime.now(clock));
        sheet.setStatus(request.status() == TimesheetApprovalStatus.REJECTED ? TimesheetStatus.REJECTED
                : primary ? TimesheetStatus.LEVEL1_APPROVED : TimesheetStatus.APPROVED);
        timesheets.saveAndFlush(sheet);
        return new TimesheetApprovalUpdateResponse(sheet.getId(), approval.getId(),
                approval.getApprovalLevel(), approval.getStatus(), sheet.getStatus(),
                approval.getComments(), approval.getActionAt());
    }
}
