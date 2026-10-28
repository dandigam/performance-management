package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;

public record RoleFeatureFlagRequest(@NotNull Boolean enabled) {}
