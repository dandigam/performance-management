package com.rit.performance.dto.request;

import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TimesheetEmployeeProjectRequest {
    @Positive
    private Long timesheetEmployeeProjectId;

    @Positive
    private Long sowId;

    @Positive
    private Long milestoneId;

    private com.rit.performance.entity.TimesheetWorkType workType = com.rit.performance.entity.TimesheetWorkType.PROJECT;
    @jakarta.validation.constraints.Size(max = 50)
    private String internalWorkType;
    @Positive
    private Long milestonePositionAssignmentId;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;

    @NotNull
    @com.fasterxml.jackson.annotation.JsonProperty("plannedStartDate")
    @com.fasterxml.jackson.annotation.JsonAlias("startDate")
    private LocalDate startDate;

    @NotNull
    @com.fasterxml.jackson.annotation.JsonProperty("plannedEndDate")
    @com.fasterxml.jackson.annotation.JsonAlias("endDate")
    private LocalDate endDate;

    @DecimalMin("0.00")
    @DecimalMax("24.00")
    private BigDecimal defaultHoursPerDay;

    @NotNull
    private List<@NotNull @Valid TimesheetScheduleDateRequest> scheduleDates = new ArrayList<>();

    @NotNull
    private List<@NotNull @Valid TimesheetDeletedDateRequest> deletedDates = new ArrayList<>();

    // Reject the previous replacement contract explicitly instead of silently ignoring it.
    @Deprecated
    private List<TimesheetDailyOverrideRequest> dailyOverrides;

    @NotNull
    @Positive
    private Long level1ApproverId;

    @NotNull
    @Positive
    private Long level2ApproverId;

    @NotNull
    private TimesheetEmployeeProjectStatus status;
}
