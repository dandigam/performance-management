package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowFeatureRequest {
    @NotBlank(message = "featureCode is required")
    @Size(max = 50, message = "featureCode must not exceed 50 characters")
    private String featureCode;

    @NotBlank(message = "featureName is required")
    @Size(max = 200, message = "featureName must not exceed 200 characters")
    private String featureName;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    @DecimalMin(value = "0.0", message = "completionPercentage must be at least 0")
    @DecimalMax(value = "100.0", message = "completionPercentage must not exceed 100")
    private BigDecimal completionPercentage;

    @DecimalMin(value = "0.0", message = "riskPercentage must be at least 0")
    @DecimalMax(value = "100.0", message = "riskPercentage must not exceed 100")
    private BigDecimal riskPercentage;

    @DecimalMin(value = "0.0", message = "productivityPercentage must be at least 0")
    @DecimalMax(value = "100.0", message = "productivityPercentage must not exceed 100")
    private BigDecimal productivityPercentage;

    @Size(max = 1000, message = "remarks must not exceed 1000 characters")
    private String remarks;

    @Min(value = 1, message = "displayOrder must be at least 1")
    private Integer displayOrder;
}
