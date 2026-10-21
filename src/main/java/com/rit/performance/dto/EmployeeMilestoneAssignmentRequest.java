package com.rit.performance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeMilestoneAssignmentRequest {
    @NotNull(message = "milestoneId is required")
    @Positive(message = "milestoneId must be positive")
    private Long milestoneId;

    @NotNull(message = "milestonePositionId is required")
    @Positive(message = "milestonePositionId must be positive")
    private Long milestonePositionId;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;

    @NotBlank(message = "positionType is required")
    @Pattern(regexp = "(?i)BILLABLE|NON[_ ]?BILLABLE",
            message = "positionType must be BILLABLE or NON_BILLABLE")
    private String positionType;

    private String status;
}
