package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoiceStatusRequest {
    @NotBlank(message = "status is required")
    @Size(max = 30)
    private String status;
    @NotNull(message = "actionDate is required")
    private LocalDate actionDate;
    @Size(max = 1000)
    private String reason;
    private Long updatedBy;
}
