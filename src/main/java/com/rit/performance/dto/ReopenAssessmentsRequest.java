package com.rit.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReopenAssessmentsRequest {

    @NotEmpty(message = "employeeIds must contain at least one employee")
    @Size(max = 500, message = "A maximum of 500 employees can be reopened at once")
    private List<@NotNull @Positive Long> employeeIds;

    @NotNull(message = "daysPerStage is required")
    @Min(value = 1, message = "daysPerStage must be at least 1")
    @Max(value = 365, message = "daysPerStage must not exceed 365")
    private Integer daysPerStage;

    @NotBlank(message = "reason is required")
    @Size(max = 2000, message = "reason must not exceed 2000 characters")
    private String reason;

    private boolean notifyAssignees;
}
