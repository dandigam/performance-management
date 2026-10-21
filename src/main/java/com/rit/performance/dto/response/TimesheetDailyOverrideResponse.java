package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetDayType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TimesheetDailyOverrideResponse {
    private LocalDate workDate;
    private BigDecimal scheduledHours;
    private TimesheetDayType dayType;
}
