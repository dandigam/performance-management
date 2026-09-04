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

    private LocalDate milestoneInvoiceDate;

    @DecimalMin(value = "0.0", message = "milestoneInvoiceAmount cannot be negative")
    private BigDecimal milestoneInvoiceAmount;

    private LocalDate invoiceRaisedDate;

    @DecimalMin(value = "0.0", message = "invoiceRaisedAmount cannot be negative")
    private BigDecimal invoiceRaisedAmount;

    @Size(max = 30, message = "invoiceStatus must not exceed 30 characters")
    private String invoiceStatus;

    private LocalDate submittedDate;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;

    private Long updatedBy;
}
