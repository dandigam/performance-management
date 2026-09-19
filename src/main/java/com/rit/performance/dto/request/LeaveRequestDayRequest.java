package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestDayRequest(
        @NotNull LocalDate leaveDate,
        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 2, fraction = 2) BigDecimal requestedHours) {}
