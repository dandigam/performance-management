package com.rit.performance.dto;

import jakarta.validation.constraints.*;

public record OnboardingRequestChangesRequest(@NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max = 5000) String comments) {}
