package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import java.util.List;

public record CyclePublishCandidatesResponse(
        Long cycleId, String cycleStatus, boolean canPublish, List<String> configurationWarnings,
        Counts counts, List<Candidate> employees) {
    public record Counts(int eligible, int enrolled, int notEnrolled, int readyToEnroll,
                         int withWarnings, int blocked) {}
    public record Candidate(Long employeeId, String employeeName, String ritId, String email,
                            boolean enrolled, Long reviewId, EmployeeReviewStatus reviewStatus,
                            Long reportingManager1Id, String reportingManager1Name,
                            Long reportingManager2Id, String reportingManager2Name,
                            boolean canPublish, List<String> reportingChainWarnings) {}
}
