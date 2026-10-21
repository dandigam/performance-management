package com.rit.performance.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SowOwnerHistoryResponse(Long id, Long sowId, String role,
        Long previousEmployeeId, String previousEmployeeName, Long employeeId, String employeeName,
        LocalDate effectiveDate, String reason, LocalDateTime changedAt, Long changedBy, boolean baseline) {}
