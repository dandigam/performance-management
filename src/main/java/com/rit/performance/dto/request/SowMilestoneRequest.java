package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestoneRequest {
    private Long id;

    @NotBlank(message = "milestoneName is required")
    @Size(max = 200, message = "milestoneName must not exceed 200 characters")
    private String milestoneName;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate invoiceDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 30, message = "milestone status must not exceed 30 characters")
    private String status;

}
