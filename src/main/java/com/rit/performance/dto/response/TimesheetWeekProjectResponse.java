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
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private List<TimesheetDailyOverrideResponse> scheduleDates;
    private String designationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxHoursPerDay;
    private List<TimesheetWeekEntryResponse> entries;
}
