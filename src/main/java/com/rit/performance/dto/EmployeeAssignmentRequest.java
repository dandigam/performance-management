package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
public class EmployeeAssignmentRequest {
    @NotNull
    @Positive
    private Long employeeId;

    @NotNull
    @Positive
    private Long sowId;

    @Positive
    private Long leadId;

    @Positive
    private Long managerId;

    private Boolean isPrimaryAssignment;

    private LocalDate effectiveFrom;
    private Long updatedBy;

    @NotEmpty(message = "milestoneAssignments must contain at least one assignment")
    @Valid
    private List<@NotNull(message = "milestoneAssignments cannot contain null values")
            EmployeeMilestoneAssignmentRequest> milestoneAssignments;
}
