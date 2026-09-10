package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TimesheetEmployeeProjectSummaryResponse {
    private Long timesheetEmployeeProjectId;
    private Long employeeId;
    private Long sowId;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal defaultHoursPerDay;
    private Long level1ApproverId;
    private String level1ApproverName;
    private Long level2ApproverId;
    private String level2ApproverName;
    private TimesheetEmployeeProjectStatus status;
}
