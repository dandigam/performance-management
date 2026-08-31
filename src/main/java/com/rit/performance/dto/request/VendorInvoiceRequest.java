package com.rit.performance.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoiceRequest {
    @NotBlank(message = "invoiceNumber is required")
    @Size(max = 100, message = "invoiceNumber must not exceed 100 characters")
    private String invoiceNumber;

    @NotNull(message = "receivedDate is required")
    private LocalDate receivedDate;

    @NotBlank(message = "invoiceType is required")
    @Size(max = 50, message = "invoiceType must not exceed 50 characters")
    private String invoiceType;

    @NotNull(message = "workOrderId is required")
    @JsonAlias("workOrderNumber")
    private Long workOrderId;

    @NotNull(message = "vendorId is required")
    private Long vendorId;

    @NotBlank(message = "location is required")
    private String location;

    @NotBlank(message = "status is required")
    private String status;

    @NotEmpty(message = "items must contain at least one invoice item")
    private List<@NotNull(message = "items cannot contain null values")
            @Valid VendorInvoiceItemRequest> items;
}
