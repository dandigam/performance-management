package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveApprovalAction;
import com.rit.performance.entity.LeaveApprovalLevel;
import java.time.LocalDateTime;

public record LeaveApprovalHistoryResponse(Long id, LeaveApprovalLevel approvalLevel,
        Long approverId, String approverName, LeaveApprovalAction action,
        String comments, LocalDateTime actionAt) {}
