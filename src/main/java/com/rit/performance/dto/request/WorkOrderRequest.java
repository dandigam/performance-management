package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrderRequest {
    @NotBlank(message = "description is required")
    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotBlank(message = "location is required")
    private String location;

    @NotNull(message = "sowId is required")
    private Long sowId;

    @DecimalMin(value = "0.0", message = "amount cannot be negative")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "hourlyRate cannot be negative")
    private BigDecimal hourlyRate;

    @DecimalMin(value = "0.0", message = "salary cannot be negative")
    private BigDecimal salary;

    @DecimalMin(value = "0.0", message = "commission cannot be negative")
    private BigDecimal commission;

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @Size(max = 2000, message = "comments must not exceed 2000 characters")
    private String comments;

    private List<@NotNull(message = "documentList cannot contain null values")
            @Valid WorkOrderDocumentRequest> documentList;
}
