package com.rit.performance.dto.request;

import com.rit.performance.entity.LeaveBalanceAdjustmentType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveBalanceAdjustmentRequest(
        @NotNull LeaveBalanceAdjustmentType adjustmentType,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 255) String reason,
        @Size(max = 1000) String notes,
        @NotNull LocalDate adjustmentDate) {}
