package com.rit.performance.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record AssignmentSowOptionResponse(Long sowId, String sowName, String status,
        List<MilestoneOption> milestones) {
    public record MilestoneOption(Long milestoneId, String milestoneName, LocalDate startDate,
            LocalDate endDate, List<PositionOption> positions) {}
    public record PositionOption(Long positionId, String positionTitle, Long designationId,
            String skillName, String seniority, String location, String positionType, String status,
            LocalDate startDate, LocalDate endDate, BigDecimal hours) {}
}
