package com.rit.performance.reports.timesheet;

public class TimesheetPdfForbiddenException extends RuntimeException {
    public TimesheetPdfForbiddenException() { super("You do not have permission to download this timesheet."); }
}
