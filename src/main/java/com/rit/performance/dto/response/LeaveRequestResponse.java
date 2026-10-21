package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveRequestStatus;
import com.rit.performance.entity.LeaveUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LeaveRequestResponse(Long id, Long employeeLeavePolicyId,
        Long leaveTypeId, String leaveTypeCode, String leaveTypeName, LeaveUnit unit,
        LocalDate fromDate, LocalDate toDate, BigDecimal totalHours, String reason,
        String notes, LeaveRequestStatus status, LocalDateTime submittedAt,
        LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy,
        List<LeaveRequestDayResponse> days) {}
