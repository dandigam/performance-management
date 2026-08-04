package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
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
public class EmployeeHierarchyMemberResponse {
    private Long employeeId;
    private String employeeName;
    private String email;
    private String phoneNumber;
    private String ritId;
    private String csxRacfId;
    private Long roleId;
    private String roleName;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;
    private Long projectId;
    private String projectName;
    private Long managerId;
    private String managerName;
    private Long leadId;
    private String leadName;
    private String status;

    private Long reviewId;
    private EmployeeReviewStatus reviewStatus;
    private BigDecimal reviewProgressPercentage;
    private EmployeeReviewStatus selfAssessmentStatus;
    private EmployeeReviewStatus teamLeadAssessmentStatus;
    private EmployeeReviewStatus managerAssessmentStatus;
    private Long assignedAssessmentId;
    private String assignedRoleName;
    private EmployeeReviewStatus assignedAssessmentStatus;
    private boolean actionRequired;

}
