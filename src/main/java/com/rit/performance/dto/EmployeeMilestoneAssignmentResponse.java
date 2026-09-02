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
public class EmployeeMilestoneAssignmentResponse {
    private Long assignmentId;
    private Long milestoneId;
    private String milestoneName;
    private Long milestonePositionId;
    private Long designationId;
    private String designationName;
    private Long seniorityId;
    private String seniority;
    private String location;
    private String positionType;
    private Integer allocationPercentage;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String assignmentStatus;
}
