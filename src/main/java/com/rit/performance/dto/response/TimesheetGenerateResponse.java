package com.rit.performance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TimesheetGenerateResponse {
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private int generatedCount;
    private List<Long> timesheetIds;
    private List<Long> employeeIds;
}
