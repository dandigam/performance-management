package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetApprovalStatus;
import com.rit.performance.entity.TimesheetStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TimesheetApprovalResponse {
    private Long approvalId;
    private Long timesheetId;
    private Long timesheetEmployeeProjectId;
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private TimesheetStatus timesheetStatus;
    private Integer approvalLevel;
    private TimesheetApprovalStatus approvalStatus;
    private BigDecimal totalHours;
    private BigDecimal regularHours;
    private BigDecimal holidayHours;
    private BigDecimal leaveHours;
    private List<TimesheetAuditLogResponse> auditLog;
    private String comments;
    private LocalDateTime submittedAt;
    private LocalDateTime actionAt;
}