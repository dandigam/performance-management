package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeReviewAssessmentResponse {
    private Long id;
    private Integer assessmentLevel;
    private Long assessorRoleId;
    private String assessorRoleName;
    private Long assessorEmployeeId;
    private String assessorEmployeeName;
    private EmployeeReviewStatus status;
    private BigDecimal progressPercentage;
    private BigDecimal overallRating;
    private String overallComment;
    private LocalDateTime startedDate;
    private LocalDateTime submittedDate;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
    private List<EmployeeReviewAnswerResponse> answers;
}
