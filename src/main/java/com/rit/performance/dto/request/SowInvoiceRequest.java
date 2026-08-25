package com.rit.performance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowInvoiceRequest {

    @NotNull(message = "milestoneId is required")
    private Long milestoneId;

    private LocalDate actualInvoiceDate;

    @DecimalMin(value = "0.0", message = "invoiceAmount cannot be negative")
    private BigDecimal invoiceAmount;

    @Size(max = 30, message = "invoiceStatus must not exceed 30 characters")
    private String invoiceStatus;

    private LocalDate submittedDate;
    private LocalDate paymentReceivedDate;

    @DecimalMin(value = "0.0", message = "receivedAmount cannot be negative")
    private BigDecimal receivedAmount;

    @Size(max = 30, message = "paymentStatus must not exceed 30 characters")
    private String paymentStatus;

    private Long updatedBy;
}
