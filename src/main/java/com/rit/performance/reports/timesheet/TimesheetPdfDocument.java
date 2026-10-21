package com.rit.performance.reports.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record TimesheetPdfDocument(Long timesheetId, String employeeNumber, String employeeName,
        String workMode, LocalDate periodStart, LocalDate periodEnd, String status,
        String statusLabel, String comments, BigDecimal regularHours, BigDecimal overtimeHours,
        BigDecimal holidayHours, BigDecimal leaveHours, BigDecimal totalHours,
        OffsetDateTime submittedAt, OffsetDateTime approvedAt, List<TimesheetPdfDayHeader> weekDays,
        List<TimesheetPdfProject> projects, List<TimesheetPdfAuditEntry> approvalHistory,
        OffsetDateTime generatedAt) {
    private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, hh:mm a", Locale.US);

    public String submittedAtLabel() {
        return submittedAt == null ? "" : submittedAt.format(SUBMITTED_AT_FORMAT);
    }
}
