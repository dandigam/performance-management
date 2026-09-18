package com.rit.performance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SowPositionSummaryResponse(
        Long positionId, String positionTitle, String location, BigDecimal estimatedHours,
        LocalDate startDate, LocalDate endDate, String status, String positionType,
        Long assignmentId, Long employeeId, String employeeName) {
}
