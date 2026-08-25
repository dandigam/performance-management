package com.rit.performance.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDate receivedDate;
    private String invoiceType;
    private Long workOrderId;
    private Long workOrderNumber;
    private Long vendorId;
    private String vendorName;
    private String location;
    private String status;
    private List<VendorInvoiceItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
