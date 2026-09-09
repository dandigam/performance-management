package com.rit.performance.dto.response;

import com.rit.performance.dto.AuditResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDate milestoneInvoiceDate;
    private BigDecimal milestoneInvoiceAmount;
    private LocalDate invoiceRaisedDate;
    private BigDecimal invoiceRaisedAmount;
    private String invoiceStatus;
    private LocalDate submittedDate;
    private String notes;
    private BigDecimal totalReceived;
    private BigDecimal balanceAmount;
    private String paymentStatus;
    private List<SowInvoicePaymentResponse> payments;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
    private AuditResponse audit;
}
