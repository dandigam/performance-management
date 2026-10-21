package com.rit.performance.dto.request;

import com.rit.performance.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record LeavePolicyRequest(
        @NotBlank @Size(max = 150) String policyName,
        @NotNull LeavePolicyCountry country,
        @NotNull EmploymentType employmentType,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LeavePolicyStatus status) {}
