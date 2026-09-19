package com.rit.performance.dto.response;

import com.rit.performance.entity.LeavePolicyStatus;
import com.rit.performance.entity.LeaveUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeLeaveBalanceResponse(
        Long id, Long employeeId, Long employeeLeavePolicyId, Long leaveTypeId,
        String leaveTypeCode, String leaveTypeName, LeaveUnit unit, int balanceYear,
        BigDecimal openingBalance, BigDecimal entitled, BigDecimal totalAdjustments,
        BigDecimal used, Object available, boolean unlimited, LeavePolicyStatus status,
        LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {}
