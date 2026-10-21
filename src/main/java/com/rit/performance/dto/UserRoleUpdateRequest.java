package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserRoleUpdateRequest(
        @NotNull(message = "roleId is required")
        @Positive(message = "roleId must be positive")
        Long roleId
) {
}
