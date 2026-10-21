package com.rit.performance.dto.request;

import com.rit.performance.entity.LeaveUnit;
import com.rit.performance.entity.LeaveTypeStatus;
import jakarta.validation.constraints.*;

public record LeaveTypeRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull LeaveUnit unit,
        @NotNull Boolean paid,
        LeaveTypeStatus status) {}
