package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LeaveRejectionRequest(@NotBlank String reason) {}
