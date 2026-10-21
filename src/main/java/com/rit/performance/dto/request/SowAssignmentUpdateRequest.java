package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowAssignmentUpdateRequest {
    private Long milestoneId;

    private Long designationId;

    private String positionType;

    private Long leadId;
    private Long managerId;

    private Boolean isPrimaryAssignment;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;

    private LocalDate assignmentEndDate;

    @NotBlank(message = "assignmentStatus is required")
    private String assignmentStatus;

    private Long updatedBy;
}
