package com.rit.performance.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoiceItemResponse {
    private Long id;
    private Integer quantity;
    private String description;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
