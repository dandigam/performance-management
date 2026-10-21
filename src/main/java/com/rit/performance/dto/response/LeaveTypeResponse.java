package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveUnit;
import com.rit.performance.entity.LeaveTypeStatus;
import java.time.LocalDateTime;

public record LeaveTypeResponse(Long id, String code, String name, String description,
        LeaveUnit unit, boolean paid, LeaveTypeStatus status, LocalDateTime createdAt,
        Long createdBy, LocalDateTime updatedAt, Long updatedBy) {}
