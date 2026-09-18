package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TimesheetSummaryResponse {
    private Long timesheetId;
    private Long timesheetEmployeeProjectId;
    private Long employeeId;
    private String employeeName;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private TimesheetStatus status;
    private String statusDisplay;
    private String approvalStatus;
    private String clientName;
    private String endClient;
    private BigDecimal totalHours;
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal totalTimeOffHours;
    private String file;
    private String commentsNotes;
    private List<TimesheetAuditLogResponse> auditLog;
}
