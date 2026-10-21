package com.rit.performance.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SowStatusHistoryResponse(Long id, Long sowId, String previousStatus, String status,
        LocalDate statusEffectiveDate, LocalDateTime changedAt, Long changedBy,
        String changedByName, LocalDateTime approvedAt, boolean baseline, String reason) {}
