package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeePerformanceResponse(Long employeeId, String employeeName, List<ReviewSummary> performance) {
    public record ReviewSummary(Long reviewId, Long cycleId, String reviewName, EmployeeReviewStatus status,
                                List<StageRating> stageRatings, BigDecimal finalRating, LocalDateTime publishedDate) {}

    public record StageRating(Long assessmentId, Integer assessmentLevel, String stageCode, String stageName,
                              Long reviewerEmployeeId, String reviewerName, EmployeeReviewStatus status,
                              BigDecimal overallRating, String overallComment, LocalDateTime submittedDate) {}
}
