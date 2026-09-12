package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TimesheetWeekResponse {
    private Long timesheetId;
    private Long employeeId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String workMode;
    private List<TimesheetWeekProjectResponse> projects;
    private TimesheetStatus status;
}
