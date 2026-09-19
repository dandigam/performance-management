package com.rit.performance.dto.request;

import com.rit.performance.entity.LeaveTypeStatus;
import jakarta.validation.constraints.NotNull;

public record LeaveTypeStatusRequest(@NotNull LeaveTypeStatus status) {}
