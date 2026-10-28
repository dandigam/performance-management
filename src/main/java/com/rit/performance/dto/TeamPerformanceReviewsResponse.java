package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import java.util.List;

public record TeamPerformanceReviewsResponse(
        Long reviewerEmployeeId,
        String reviewerEmployeeName,
        Long cycleId,
        String cycleName,
        List<Review> reviews) {

    public record Review(Long employeeId, String employeeName, String ritId,
                         String designationName, Long reviewId, Long assessmentId,
                         String stage, EmployeeReviewStatus status, String action) {
    }
}
