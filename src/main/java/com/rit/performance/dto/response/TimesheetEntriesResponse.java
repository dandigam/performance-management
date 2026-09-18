package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetStatus;
import java.math.BigDecimal;
import java.util.List;

public record TimesheetEntriesResponse(Long timesheetId, TimesheetStatus status,
        BigDecimal regularHours, BigDecimal holidayHours, BigDecimal leaveHours,
        BigDecimal totalHours, List<TimesheetWeekEntryResponse> entries) {}
