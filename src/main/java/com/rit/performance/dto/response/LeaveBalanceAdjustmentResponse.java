package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveBalanceAdjustmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveBalanceAdjustmentResponse(
        Long id, Long employeeLeaveBalanceId, LeaveBalanceAdjustmentType adjustmentType,
        BigDecimal amount, String reason, String notes, LocalDate adjustmentDate,
        LocalDateTime createdAt, Long createdBy) {}
