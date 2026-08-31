package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSowAssignmentResponse {
    private Long employeeAssignmentId;
    private Long sowId;
    private String sowCode;
    private String sowName;
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
    private List<EmployeeMilestoneAssignmentResponse> milestoneAssignments;
}
