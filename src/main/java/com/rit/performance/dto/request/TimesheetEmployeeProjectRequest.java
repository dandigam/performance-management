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

    @NotNull
    @Positive
    private Long sowId;

    @NotNull
    @Positive
    private Long milestoneId;

    @NotNull
    private LocalDate startDate;

    @NotNull
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
