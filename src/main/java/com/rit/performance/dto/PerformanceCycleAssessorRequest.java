package com.rit.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceCycleAssessorRequest {

    @NotNull(message = "roleId is required")
    private Long roleId;

    @NotNull(message = "actionTypeId is required")
    private Long actionTypeId;

    @NotNull(message = "weightage is required")
    @DecimalMin(value = "0.00", message = "weightage must be at least 0")
    @DecimalMax(value = "100.00", message = "weightage must not exceed 100")
    private BigDecimal weightage;

    @NotNull(message = "displayOrder is required")
    private Integer displayOrder;
}
