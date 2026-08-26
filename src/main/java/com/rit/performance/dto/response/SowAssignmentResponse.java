package com.rit.performance.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowAssignmentResponse {
    private Long assignmentId;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String email;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long milestoneId;
    private String milestoneName;
    private Long designationId;
    private String designationName;
    private String positionType;
    private Long leadId;
    private String leadName;
    private Long managerId;
    private String managerName;
    private Integer allocationPercentage;
    private Boolean isPrimaryAssignment;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String assignmentStatus;
}
