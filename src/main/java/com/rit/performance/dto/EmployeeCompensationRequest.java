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
    @NotBlank
    @Size(max = 30)
    private String payType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal hourlyRate;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotNull
    private LocalDate effectiveDate;

    @Size(max = 500, message = "reason must not exceed 500 characters")
    @JsonAlias("changeReason")
    private String reason;
}
