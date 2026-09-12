package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class TimesheetEmployeeProjectResponse {
    private Long timesheetEmployeeProjectId;
    private Long employeeId;
    private String employeeName;
    private Long sowId;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private String designationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal defaultHoursPerDay;
    private List<TimesheetDailyOverrideResponse> scheduleDates;
    private Long level1ApproverId;
    private String level1ApproverName;
    private Long level2ApproverId;
    private String level2ApproverName;
    private TimesheetEmployeeProjectStatus status;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
