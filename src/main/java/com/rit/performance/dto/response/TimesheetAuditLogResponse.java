package com.rit.performance.dto.response;

import java.time.LocalDateTime;

/** A recorded submission or approval action, not a pending approval assignment. */
public record TimesheetAuditLogResponse(String action, String stage, Integer approvalLevel,
        Long employeeId, String employeeName, LocalDateTime actionAt, String comments) {}
