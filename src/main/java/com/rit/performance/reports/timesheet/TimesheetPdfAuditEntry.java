package com.rit.performance.reports.timesheet;

import java.time.OffsetDateTime;

public record TimesheetPdfAuditEntry(String action, String stage, Integer approvalLevel,
                                     String employeeName, OffsetDateTime actionAt, String comments) {}
