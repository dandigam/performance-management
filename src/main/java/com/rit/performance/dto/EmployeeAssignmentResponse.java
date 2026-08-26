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
public class EmployeeAssignmentResponse {
    private Long assignmentId;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private Long designationId;
    private String designationName;
    private String positionType;
    private Boolean isPrimaryAssignment;
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
}
