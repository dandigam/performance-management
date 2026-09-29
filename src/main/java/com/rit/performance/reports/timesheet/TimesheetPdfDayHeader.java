package com.rit.performance.reports.timesheet;

import java.time.LocalDate;

public record TimesheetPdfDayHeader(LocalDate date, String label) {}
