package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestoneUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String milestoneName;
    @Size(max = 2000)
    private String description;
    @Size(max = 2000)
    private String deliverables;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private LocalDate invoiceDate;
    @DecimalMin("0.0")
    private BigDecimal invoiceAmount;
    @Size(max = 30)
    private String status;
}
