package com.rit.performance.dto.response;

import java.time.LocalDate;

public record SowMilestoneSummaryResponse(
        Long milestoneId, String milestoneName, LocalDate startDate, LocalDate endDate,
        int totalPositionCount, int openPositionCount, Integer displayOrder) {
}
