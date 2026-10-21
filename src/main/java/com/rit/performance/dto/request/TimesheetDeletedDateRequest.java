package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TimesheetDeletedDateRequest {
    @NotNull
    private LocalDate workDate;
}
