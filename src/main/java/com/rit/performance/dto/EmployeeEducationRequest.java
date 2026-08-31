package com.rit.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeEducationRequest {
    @NotBlank(message = "educationType is required")
    @Size(max = 100, message = "educationType must not exceed 100 characters")
    private String educationType;

    @NotBlank(message = "collegeUniversity is required")
    @Size(max = 250, message = "collegeUniversity must not exceed 250 characters")
    private String collegeUniversity;

    @NotNull(message = "passingYear is required")
    @Min(value = 1900, message = "passingYear must be 1900 or later")
    @Max(value = 9999, message = "passingYear must be a four-digit year")
    private Integer passingYear;

    @NotNull(message = "percentage is required")
    @DecimalMin(value = "0.00", message = "percentage must be at least 0")
    @DecimalMax(value = "100.00", message = "percentage must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "percentage supports up to two decimal places")
    private BigDecimal percentage;
}
