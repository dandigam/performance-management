package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.rit.performance.entity.LeavePolicyStatus;
import java.time.LocalDate;

public record EmployeeLeavePolicyRequest(
        @NotNull Long leavePolicyId,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotNull @Positive Long level1ApproverId,
        @Positive Long level2ApproverId,
        LeavePolicyStatus status) {}
