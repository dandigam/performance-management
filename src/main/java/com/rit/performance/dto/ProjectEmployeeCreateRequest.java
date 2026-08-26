package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectEmployeeCreateRequest {
    @NotNull(message = "employeeId is required")
    @Positive(message = "employeeId must be positive")
    private Long employeeId;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;
}
