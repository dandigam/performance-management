package com.rit.performance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TimesheetWeekDayResponse {
    private LocalDate date;
    private Long holidayId;
    private String holidayName;
    private BigDecimal leaveHours;
}
