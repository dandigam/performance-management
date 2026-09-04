package com.rit.performance.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoiceAuditHistoryResponse {
    private Long invoiceId;
    private String invoiceNumber;
    private SowInvoiceResponse invoiceDetails;
    private List<SowInvoiceHistoryResponse> invoiceHistory;
    private List<SowInvoicePaymentHistoryResponse> paymentHistory;
}
