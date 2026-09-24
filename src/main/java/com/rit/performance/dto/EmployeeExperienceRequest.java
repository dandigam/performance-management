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
    private String companyName;

    private String position;

    private String location;

    private LocalDate fromDate;

    private LocalDate endDate;

    @JsonIgnore
    public boolean isDateRangeValid() {
        return fromDate == null || endDate == null || !endDate.isBefore(fromDate);
    }
}
