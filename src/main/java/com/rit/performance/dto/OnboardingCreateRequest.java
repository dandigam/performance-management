package com.rit.performance.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record OnboardingCreateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 50) String email,
        @NotNull LocalDate joiningDate,
        @NotNull @Positive Long roleId,
        @NotNull @Positive Long designationId,
        @NotBlank String employmentType,
        @NotBlank String workMode,
        @NotBlank String workLocation,
        @Positive Long vendorId) {}
