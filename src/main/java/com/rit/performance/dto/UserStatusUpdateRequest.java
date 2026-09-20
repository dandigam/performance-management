package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "(?i)ACTIVE|INACTIVE", message = "status must be ACTIVE or INACTIVE")
        String status
) {
}
