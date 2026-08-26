package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectEmployeeStatusUpdateRequest {
    @NotBlank(message = "status is required")
    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;
}
