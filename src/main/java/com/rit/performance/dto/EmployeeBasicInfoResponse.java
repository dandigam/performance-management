package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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
    private String employmentType;
    private String workMode;
    private Long vendorId;
    private String vendorCode;
    private String vendorCompanyName;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long designationId;
    private String designationName;
    private Long assignmentId;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Integer allocationPercentage;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String assignmentStatus;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private Long leadId;
    private String leadName;
    private String status;
    private EmployeeReviewSummaryResponse review;
}
