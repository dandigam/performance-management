package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeLeavePolicyRequest(
        @NotNull Long leavePolicyId,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo) {}
