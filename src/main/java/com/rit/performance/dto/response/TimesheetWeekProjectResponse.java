package com.rit.performance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TimesheetWeekProjectResponse {
    private Long timesheetEmployeeProjectId;
    private Long sowId;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private List<TimesheetDailyOverrideResponse> scheduleDates;
    private String designationName;
    @com.fasterxml.jackson.annotation.JsonProperty("plannedStartDate")
    private LocalDate startDate;
    @com.fasterxml.jackson.annotation.JsonProperty("plannedEndDate")
    private LocalDate endDate;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private Long milestonePositionAssignmentId;
    private com.rit.performance.entity.TimesheetWorkType workType;
    private String internalWorkType;
    private Integer maxHoursPerDay;
    private List<TimesheetWeekEntryResponse> entries;
    // Legacy aliases retain planned-date semantics during client migration.
    @com.fasterxml.jackson.annotation.JsonProperty("startDate")
    public LocalDate legacyStartDate() { return startDate; }
    @com.fasterxml.jackson.annotation.JsonProperty("endDate")
    public LocalDate legacyEndDate() { return endDate; }
}
