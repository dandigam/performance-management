package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TimesheetHistoryGenerateRequest {
    @NotNull
    @Positive
    private Long employeeId;

    @NotNull
    private LocalDate accessPeriodStartDate;

    private LocalDate previousAccessPeriodStartDate;
}
