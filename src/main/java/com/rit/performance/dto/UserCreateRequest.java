package com.rit.performance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Long roleId,
        Long employeeId,
        boolean sendInvitation,
        @jakarta.validation.constraints.Pattern(regexp = "FULL|ONBOARDING|BLOCKED",
                message = "portalAccess must be FULL, ONBOARDING or BLOCKED")
        String portalAccess) {
    public UserCreateRequest(String username, String email, Long roleId, Long employeeId,
                             boolean sendInvitation) {
        this(username, email, roleId, employeeId, sendInvitation, null);
    }
}
