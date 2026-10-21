package com.rit.performance.dto.request;

import com.rit.performance.entity.TimesheetEntryType;
import com.rit.performance.entity.TimesheetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TimesheetEntriesRequest(List<Entry> entries, TimesheetStatus status) {
    public TimesheetEntriesRequest(List<Entry> entries) {
        this(entries, null);
    }

    public record Entry(LocalDate workDate, TimesheetEntryType entryType,
                        BigDecimal hours, Long holidayId, Long leaveId) {}
}
