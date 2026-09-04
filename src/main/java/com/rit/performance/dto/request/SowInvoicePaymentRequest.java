package com.rit.performance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoicePaymentRequest {
    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @NotNull(message = "receivedAmount is required")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "receivedAmount must be greater than zero")
    private BigDecimal receivedAmount;

    @Size(max = 100, message = "paymentReference must not exceed 100 characters")
    private String paymentReference;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;

    private Long updatedBy;
}
