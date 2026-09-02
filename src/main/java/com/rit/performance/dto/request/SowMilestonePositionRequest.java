package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionRequest {
    private Long milestonePositionId;
    @NotNull(message = "positionId is required")
    private Long positionId;
    private Long skillId;
    private Long rateCardId;
    @DecimalMin(value = "0.0", message = "hourlyRate cannot be negative")
    private BigDecimal hourlyRate;
    @Size(max = 1000, message = "rateOverrideReason must not exceed 1000 characters")
    private String rateOverrideReason;
    private Long rateUpdatedBy;
    @Size(max = 200, message = "positionName must not exceed 200 characters")
    private String positionName;
    @NotNull(message = "seniorityId is required")
    @Positive(message = "seniorityId must be positive")
    private Long seniorityId;
    @NotBlank(message = "positionType is required")
    @Size(max = 20, message = "positionType must not exceed 20 characters")
    private String positionType;
    @Size(max = 20, message = "locationType must not exceed 20 characters")
    @JsonProperty("location")
    private String locationType;
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 50, message = "hours must not exceed 50 characters")
    private String hours;
    @DecimalMin(value = "0.0", message = "amount cannot be negative")
    private BigDecimal amount;
}
