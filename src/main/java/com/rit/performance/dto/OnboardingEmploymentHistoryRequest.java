package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record OnboardingEmploymentHistoryRequest(
        @NotEmpty List<@NotNull @Valid ExperienceDetail> experienceDetails) {
    public record ExperienceDetail(
            @NotBlank @Size(max = 255) String companyName,
            @NotBlank @Size(max = 255) String position,
            @NotBlank @Size(max = 255) String location,
            @NotNull LocalDate fromDate,
            LocalDate endDate) {
        @AssertTrue(message = "endDate must be on or after fromDate")
        public boolean isDateRangeValid() {
            return fromDate == null || endDate == null || !endDate.isBefore(fromDate);
        }
    }
}
