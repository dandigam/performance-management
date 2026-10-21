package com.rit.performance.dto.request;

import com.rit.performance.entity.LeavePolicyStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LeavePolicyRuleRequest(
        @NotNull Long leaveTypeId,
        @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 8, fraction = 2) BigDecimal entitlement,
        @NotNull Boolean carryForward,
        @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 8, fraction = 2) BigDecimal maxCarryForward,
        LeavePolicyStatus status) {}
