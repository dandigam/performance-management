package com.rit.performance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record InitializeLeaveBalancesRequest(
        @NotNull Long employeeLeavePolicyId,
        @Min(1000) @Max(9999) int balanceYear) {}
