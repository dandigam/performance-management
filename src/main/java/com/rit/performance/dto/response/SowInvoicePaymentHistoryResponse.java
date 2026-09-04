package com.rit.performance.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoicePaymentHistoryResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private Long paymentId;
    private LocalDate paymentDate;
    private BigDecimal receivedAmount;
    private String paymentReference;
    private String paymentMethod;
    private String notes;
    private String action;
    private Long changedBy;
    private String changedByName;
    private LocalDateTime changedOn;
}
