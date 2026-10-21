package com.rit.performance.dto.request;

import com.rit.performance.entity.TimesheetDayType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class TimesheetDailyOverrideRequest {
    @NotNull
    private LocalDate workDate;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("24.00")
    private BigDecimal scheduledHours;

    private TimesheetDayType dayType;
}
