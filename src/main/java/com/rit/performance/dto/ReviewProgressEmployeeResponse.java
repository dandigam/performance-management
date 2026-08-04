package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewProgressEmployeeResponse {
    private Long employeeId;
    private String employeeName;
    private String designationName;
    private String departmentName;
    private String projectName;
    private Long managerId;
    private String managerName;
    private Long leadId;
    private String leadName;
    private Long reviewId;
    private String currentStage;
    private Long pendingWithEmployeeId;
    private String pendingWithEmployeeName;
    private String pendingWithRole;
    private LocalDateTime lastUpdatedAt;
    private EmployeeReviewStatus reviewStatus;
    private BigDecimal progressPercentage;
    private EmployeeReviewStatus selfAssessmentStatus;
    private EmployeeReviewStatus teamLeadAssessmentStatus;
    private EmployeeReviewStatus managerAssessmentStatus;
    private List<ReviewProgressAssessmentResponse> assessments;
    private boolean ratingPublished;
}
