package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetApprovalStatus;
import com.rit.performance.entity.TimesheetStatus;
import java.time.LocalDateTime;

public record TimesheetApprovalUpdateResponse(Long timesheetId, Long approvalId,
        Integer approvalLevel, TimesheetApprovalStatus approvalStatus,
        TimesheetStatus timesheetStatus, String comments, LocalDateTime actionAt) {}
