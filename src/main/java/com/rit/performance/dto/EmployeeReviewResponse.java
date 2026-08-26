package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReviewResponse {
    private Long id;
    private Long employeeId;
    private Long cycleId;
    private EmployeeReviewStatus status;
    private BigDecimal progressPercentage;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
    private List<EmployeeReviewAssessmentResponse> assessmentStages;
}
