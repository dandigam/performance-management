package com.rit.performance.dto;

import jakarta.validation.constraints.Pattern;

public record UserPortalAccessUpdateRequest(
        @Pattern(regexp = "FULL|ONBOARDING|BLOCKED",
                message = "portalAccess must be FULL, ONBOARDING or BLOCKED")
        String portalAccess) {
}
