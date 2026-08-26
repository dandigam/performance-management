package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestoneRequest {
    private Long id;

    @NotBlank(message = "milestoneName is required")
    @Size(max = 200, message = "milestoneName must not exceed 200 characters")
    private String milestoneName;

    @Size(max = 2000, message = "milestone description must not exceed 2000 characters")
    private String description;

    @Min(value = 0, message = "estimatedHours cannot be negative")
    private Integer estimatedHours;

    @NotNull(message = "milestone startDate is required")
    private LocalDate startDate;

    @NotNull(message = "milestone endDate is required")
    private LocalDate endDate;
    private LocalDate invoiceDate;

    @DecimalMin(value = "0.0", message = "invoiceAmount cannot be negative")
    @JsonProperty("invoiceAmount")
    @JsonAlias("amount")
    private BigDecimal amount;

    @Size(max = 30, message = "milestone status must not exceed 30 characters")
    private String status;

    @Valid
    private List<SowMilestonePositionRequest> positions;

}
