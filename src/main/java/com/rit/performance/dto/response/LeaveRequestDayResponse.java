package com.rit.performance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestDayResponse(Long id, LocalDate leaveDate,
        BigDecimal scheduledHours, BigDecimal requestedHours) {}
