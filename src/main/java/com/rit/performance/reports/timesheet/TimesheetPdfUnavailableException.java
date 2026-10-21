package com.rit.performance.reports.timesheet;

public class TimesheetPdfUnavailableException extends RuntimeException {
    public TimesheetPdfUnavailableException() {
        super("Submit the timesheet before downloading its PDF.");
    }
}
