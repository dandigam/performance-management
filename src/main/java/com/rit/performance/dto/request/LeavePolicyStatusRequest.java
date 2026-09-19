package com.rit.performance.dto.request;

import com.rit.performance.entity.LeavePolicyStatus;
import jakarta.validation.constraints.NotNull;

public record LeavePolicyStatusRequest(@NotNull LeavePolicyStatus status) {}
