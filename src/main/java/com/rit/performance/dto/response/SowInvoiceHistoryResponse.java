package com.rit.performance.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoiceHistoryResponse {
    private Long id;
    private Long invoiceId;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private LocalDate milestoneInvoiceDate;
    private BigDecimal milestoneInvoiceAmount;
    private LocalDate invoiceRaisedDate;
    private BigDecimal invoiceRaisedAmount;
    private String invoiceNumber;
    private String invoiceStatus;
    private LocalDate submittedDate;
    private String notes;
    private String action;
    private Long changedBy;
    private String changedByName;
    private LocalDateTime changedOn;
}
