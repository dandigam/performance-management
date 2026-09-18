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
    @com.fasterxml.jackson.annotation.JsonProperty("plannedStartDate")
    private LocalDate startDate;
    @com.fasterxml.jackson.annotation.JsonProperty("plannedEndDate")
    private LocalDate endDate;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private Long milestonePositionAssignmentId;
    private com.rit.performance.entity.TimesheetWorkType workType;
    private String internalWorkType;
    private BigDecimal defaultHoursPerDay;
    private Long level1ApproverId;
    private String level1ApproverName;
    private Long level2ApproverId;
    private String level2ApproverName;
    private TimesheetEmployeeProjectStatus status;
    // Legacy aliases retain planned-date semantics during client migration.
    @com.fasterxml.jackson.annotation.JsonProperty("startDate")
    public LocalDate legacyStartDate() { return startDate; }
    @com.fasterxml.jackson.annotation.JsonProperty("endDate")
    public LocalDate legacyEndDate() { return endDate; }
}
