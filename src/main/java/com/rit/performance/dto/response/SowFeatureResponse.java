package com.rit.performance.dto.response;

import lombok.*;

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
    private Long milestoneId;
    private String milestoneName;
    private LocalDate milestoneStartDate;
    private LocalDate milestoneEndDate;
    private String featureCode;
    private String featureName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
