package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamReviewMemberResponse {
    private Long employeeId;
    private String employeeName;
    private Long reviewId;
    private EmployeeReviewStatus reviewStatus;
    private Long selfAssessmentId;
    private EmployeeReviewStatus selfAssessmentStatus;
    private Long assignedAssessmentId;
    private Integer assignedAssessmentLevel;
    private Long assignedRoleId;
    private String assignedRoleName;
    private EmployeeReviewStatus assignedAssessmentStatus;
    private BigDecimal assignedProgressPercentage;
    private String workflowState;
    private boolean actionRequired;
}
