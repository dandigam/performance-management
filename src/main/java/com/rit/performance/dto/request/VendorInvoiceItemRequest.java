package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoiceItemRequest {
    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than zero")
    private Integer quantity;

    @NotBlank(message = "description is required")
    @Size(max = 1000, message = "description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", message = "unitPrice cannot be negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "amount cannot be negative")
    private BigDecimal amount;
}
