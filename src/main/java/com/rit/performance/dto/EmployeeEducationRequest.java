package com.rit.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeEducationRequest {
    @Size(max = 100, message = "educationType must not exceed 100 characters")
    private String educationType;

    @Size(max = 250, message = "collegeUniversity must not exceed 250 characters")
    private String collegeUniversity;

    private Integer passingYear;

    @DecimalMin(value = "0.00", message = "percentage must be at least 0")
    @DecimalMax(value = "100.00", message = "percentage must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "percentage supports up to two decimal places")
    private BigDecimal percentage;
}
