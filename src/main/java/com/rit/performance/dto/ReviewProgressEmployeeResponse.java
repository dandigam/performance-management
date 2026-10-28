package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private Long reportingManager2Id;
    private String reportingManager2Name;
    private Long reportingManager1Id;
    private String reportingManager1Name;
    private Long reviewId;
    private String currentStage;
    private Long pendingWithEmployeeId;
    private String pendingWithEmployeeName;
    private String pendingWithRole;
    private OffsetDateTime lastUpdatedAt;
    private EmployeeReviewStatus reviewStatus;
    private BigDecimal progressPercentage;
    private EmployeeReviewStatus selfAssessmentStatus;
    private EmployeeReviewStatus reportingManager1AssessmentStatus;
    private EmployeeReviewStatus reportingManager2AssessmentStatus;
    private List<ReviewProgressAssessmentResponse> assessments;
    private boolean ratingPublished;
}
