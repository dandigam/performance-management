package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowProgressSummaryResponse {
    private Long sowId;
    private BigDecimal overallCompletion;
    private int totalFeatures;
    private int completedFeatures;
    private int inProgressFeatures;
    private int notStartedFeatures;
    private int highRiskFeatures;
    private BigDecimal averageRisk;
    private BigDecimal averageProductivity;
}
