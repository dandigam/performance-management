package com.rit.performance.dto.response;

import com.rit.performance.entity.LeavePolicyStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeavePolicyRuleResponse(Long id, Long leaveTypeId, String leaveTypeCode, String leaveTypeName,
        BigDecimal entitlement, boolean carryForward, BigDecimal maxCarryForward, LeavePolicyStatus status,
        LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {}
