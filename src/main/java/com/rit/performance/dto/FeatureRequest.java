package com.rit.performance.dto;

import jakarta.validation.constraints.*;

public record FeatureRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[a-z][a-z0-9_]*",
                message = "must be lowercase letters, digits or underscores, starting with a letter") String code,
        @NotBlank @Size(max = 100) String name) {}
