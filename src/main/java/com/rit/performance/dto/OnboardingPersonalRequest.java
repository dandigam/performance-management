package com.rit.performance.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record OnboardingPersonalRequest(
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}") String phoneNumber,
        @NotBlank @Pattern(regexp = "MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY") String gender,
        @NotNull @Past LocalDate dateOfBirth) {}
