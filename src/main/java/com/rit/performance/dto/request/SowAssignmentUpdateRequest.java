package com.rit.performance.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "allocationPercentage must be at least 1")
    @Max(value = 100, message = "allocationPercentage must not exceed 100")
    private Integer allocationPercentage;

    private Boolean isPrimaryAssignment;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;

    private LocalDate assignmentEndDate;

    @NotBlank(message = "assignmentStatus is required")
    private String assignmentStatus;

    private Long updatedBy;
}
