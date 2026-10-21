package com.rit.performance.reports.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimesheetPdfDay(LocalDate date, String dayLabel, BigDecimal regularHours,
                              BigDecimal overtimeHours, BigDecimal holidayHours,
                              BigDecimal leaveHours, BigDecimal totalHours) {}
