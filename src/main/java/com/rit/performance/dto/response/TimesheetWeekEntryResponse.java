package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetEntryType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TimesheetWeekEntryResponse {
    private Long entryId;
    private LocalDate workDate;
    private TimesheetEntryType entryType;
    private BigDecimal hours;
    private Long jobId;
    private Long sowId;
    private Long leaveId;
    private Long holidayId;
}
