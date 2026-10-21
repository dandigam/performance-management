package com.rit.performance.reports.timesheet;

public class TimesheetPdfNotFoundException extends RuntimeException {
    public TimesheetPdfNotFoundException() { super("The requested timesheet was not found."); }
}
