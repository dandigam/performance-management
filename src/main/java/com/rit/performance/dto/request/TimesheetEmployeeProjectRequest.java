package com.rit.performance.dto.request;

import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TimesheetEmployeeProjectRequest {
    @Positive
    private Long timesheetEmployeeProjectId;

    @NotNull
    @Positive
    private Long sowId;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    @Min(1)
    @Max(24)
    private Integer maxHoursPerDay;

    @NotNull
    @Positive
    private Long level1ApproverId;

    @NotNull
    @Positive
    private Long level2ApproverId;

    @NotNull
    private TimesheetEmployeeProjectStatus status;
}
