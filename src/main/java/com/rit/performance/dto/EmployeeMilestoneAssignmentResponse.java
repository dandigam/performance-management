package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeMilestoneAssignmentResponse {
    private Long assignmentId;
    private Long milestonePositionAssignmentId;
    private Long milestoneId;
    private String milestoneName;
    private LocalDate milestoneStartDate;
    private LocalDate milestoneEndDate;
    private Long milestoneDurationDays;
    private Long milestonePositionId;
    private LocalDate positionStartDate;
    private LocalDate positionEndDate;
    private Long positionDurationDays;
    private String hours;
    private Long designationId;
    private String designationName;
    private Long skillId;
    private String skillName;
    private Long seniorityId;
    private String seniority;
    private String location;
    private String positionType;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private Long assignmentDurationDays;
    private String assignmentStatus;
    private Long assignedByUserId;
    private String assignedByName;
    private LocalDateTime assignedAt;
}
