package com.rit.performance.service;

import com.rit.performance.dto.EmployeeReviewRequest;
import com.rit.performance.dto.EmployeeReviewResponse;
import com.rit.performance.dto.EmployeeCycleReviewResponse;
import com.rit.performance.dto.EmployeeAssessmentRequest;
import com.rit.performance.dto.TeamReviewDashboardResponse;
import com.rit.performance.dto.ReviewerAssessmentUpdateRequest;
import com.rit.performance.dto.ReviewProgressResponse;
import com.rit.performance.dto.ReopenAssessmentsRequest;

import java.util.List;

public interface EmployeeReviewService {

    EmployeeReviewResponse getReviewById(Long id);

    List<EmployeeCycleReviewResponse> getEmployeeCycles(Long employeeId);

    EmployeeReviewResponse getEmployeeReview(Long employeeId, Long cycleId, Long assessorId);

    EmployeeReviewResponse startReview(Long employeeId, Long cycleId);

    EmployeeReviewResponse saveAssessment(EmployeeAssessmentRequest request);

    EmployeeReviewResponse updateReviewerAssessment(Long assessorEmployeeId, Long employeeId, Long cycleId,
            ReviewerAssessmentUpdateRequest request);

    EmployeeReviewResponse updateReview(Long id, EmployeeReviewRequest request);

    EmployeeReviewResponse submitReview(Long id);

    TeamReviewDashboardResponse getTeamReviews(Long assessorEmployeeId, Long cycleId);

    ReviewProgressResponse getReviewProgress(Long cycleId);

    ReviewProgressResponse reopenAssessments(Long cycleId, ReopenAssessmentsRequest request);
}
