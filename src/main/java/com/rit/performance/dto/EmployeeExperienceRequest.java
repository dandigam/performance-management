package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeExperienceRequest {
    @NotBlank(message = "companyName is required")
    @Size(max = 200, message = "companyName must not exceed 200 characters")
    private String companyName;

    @NotBlank(message = "position is required")
    @Size(max = 150, message = "position must not exceed 150 characters")
    private String position;

    @NotBlank(message = "location is required")
    @Size(max = 150, message = "location must not exceed 150 characters")
    private String location;

    @NotNull(message = "fromDate is required")
    @PastOrPresent(message = "fromDate cannot be in the future")
    private LocalDate fromDate;

    @PastOrPresent(message = "endDate cannot be in the future")
    private LocalDate endDate;

    @JsonIgnore
    @AssertTrue(message = "endDate cannot be before fromDate")
    public boolean isDateRangeValid() {
        return fromDate == null || endDate == null || !endDate.isBefore(fromDate);
    }
}
