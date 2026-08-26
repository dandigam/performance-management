package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowInvoiceResponse {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private LocalDate expectedCompletionDate;
    private LocalDate expectedInvoiceDate;
    private BigDecimal expectedAmount;
    private LocalDate actualInvoiceDate;
    private BigDecimal invoiceAmount;
    private String invoiceStatus;
    private LocalDate submittedDate;
    private LocalDate paymentReceivedDate;
    private BigDecimal receivedAmount;
    private String paymentStatus;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
