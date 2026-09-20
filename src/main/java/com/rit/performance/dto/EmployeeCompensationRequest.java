package com.rit.performance.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonAlias;

@Getter
@Setter
public class EmployeeCompensationRequest {
    @Size(max = 30)
    private String payType;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal hourlyRate;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 12, fraction = 2)
    private BigDecimal annualSalary;

    private String currency;

    private LocalDate effectiveDate;

    @Size(max = 500, message = "reason must not exceed 500 characters")
    @JsonAlias("changeReason")
    private String reason;
}
