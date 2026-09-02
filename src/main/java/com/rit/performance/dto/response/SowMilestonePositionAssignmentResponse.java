package com.rit.performance.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionAssignmentResponse {
    private Long id;
    private Long employeeAssignmentId;
    private Long employeeId;
    private String employeeName;
    private Long sowId;
    private Long milestoneId;
    private Long milestonePositionId;
    private Long positionId;
    private String positionName;
    private Long seniorityId;
    private String seniority;
    private Long rateCardId;
    private Integer allocationPercentage;
    private String positionType;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
