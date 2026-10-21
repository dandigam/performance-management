package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionAssignmentRequest {
    @NotNull(message = "employeeAssignmentId is required")
    @Positive(message = "employeeAssignmentId must be positive")
    private Long employeeAssignmentId;

    @NotBlank(message = "positionType is required")
    private String positionType;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;

    @NotBlank(message = "status is required")
    private String status;
    private Long updatedBy;
}
