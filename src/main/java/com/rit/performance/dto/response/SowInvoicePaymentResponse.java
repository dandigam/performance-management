package com.rit.performance.dto.response;

import com.rit.performance.dto.AuditResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoicePaymentResponse {
    private Long id;
    private Long invoiceId;
    private LocalDate paymentDate;
    private BigDecimal receivedAmount;
    private String paymentReference;
    private String paymentMethod;
    private String notes;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
    private AuditResponse audit;
}
