package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowFeatureResponse {
    private Long id;
    private Long sowId;
    private String featureCode;
    private String featureName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal completionPercentage;
    private BigDecimal riskPercentage;
    private BigDecimal productivityPercentage;
    private String remarks;
    private Integer displayOrder;
    private boolean active;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
