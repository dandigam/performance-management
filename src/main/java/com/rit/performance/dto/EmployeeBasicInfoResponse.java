package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeBasicInfoResponse {
    private Long employeeId;
    private String employeeName;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String ritId;
    private String csxRacfId;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long designationId;
    private String designationName;
    private Long projectId;
    private String projectName;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private Long leadId;
    private String leadName;
    private String status;
    private EmployeeReviewSummaryResponse review;
}
