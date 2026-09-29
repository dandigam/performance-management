package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OnboardingEducationRequest(
        @NotEmpty List<@NotNull @Valid EducationDetail> educationDetails) {
    public record EducationDetail(
            @NotBlank @Size(max = 100) String degree,
            @NotBlank @Size(max = 250) String institution,
            @NotNull @Min(1900) @Max(9999) Integer passingYear,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
            BigDecimal percentage) {}
}
