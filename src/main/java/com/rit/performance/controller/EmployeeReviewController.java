package com.rit.performance.controller;

import com.rit.performance.dto.EmployeeReviewRequest;
import com.rit.performance.dto.EmployeeReviewResponse;
import com.rit.performance.dto.EmployeeCycleReviewResponse;
import com.rit.performance.dto.EmployeeAssessmentRequest;
import com.rit.performance.dto.TeamReviewDashboardResponse;
import com.rit.performance.dto.ReviewerAssessmentUpdateRequest;
import com.rit.performance.dto.ReviewProgressResponse;
import com.rit.performance.dto.ReopenAssessmentsRequest;
import com.rit.performance.service.EmployeeReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/employee-reviews")
public class EmployeeReviewController {

    private final EmployeeReviewService service;

    public EmployeeReviewController(EmployeeReviewService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeReviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getReviewById(id));
    }

    @GetMapping("/employee/{employeeId}/cycles")
    public ResponseEntity<List<EmployeeCycleReviewResponse>> getEmployeeCycles(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(service.getEmployeeCycles(employeeId));
    }

    @GetMapping("/employee/{employeeId}/cycles/{cycleId}")
    public ResponseEntity<EmployeeReviewResponse> getEmployeeReview(
            @PathVariable Long employeeId,
            @PathVariable Long cycleId,
            @RequestParam(required = false) Long assessorId) {
        return ResponseEntity.ok(service.getEmployeeReview(employeeId, cycleId, assessorId));
    }

    @GetMapping("/assessor/{assessorEmployeeId}/team")
    public ResponseEntity<TeamReviewDashboardResponse> getTeamReviews(
            @PathVariable Long assessorEmployeeId,
            @RequestParam Long cycleId) {
        return ResponseEntity.ok(service.getTeamReviews(assessorEmployeeId, cycleId));
    }

    @GetMapping("/progress")
    public ResponseEntity<ReviewProgressResponse> getReviewProgress(@RequestParam Long cycleId) {
        return ResponseEntity.ok(service.getReviewProgress(cycleId));
    }

    @PostMapping("/cycles/{cycleId}/reopen-assessments")
    public ResponseEntity<ReviewProgressResponse> reopenAssessments(
            @PathVariable Long cycleId,
            @Valid @RequestBody ReopenAssessmentsRequest request) {
        return ResponseEntity.ok(service.reopenAssessments(cycleId, request));
    }

    @PostMapping("/employee/{employeeId}/cycles/{cycleId}/start")
    public ResponseEntity<EmployeeReviewResponse> start(
            @PathVariable Long employeeId,
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(service.startReview(employeeId, cycleId));
    }

    @PostMapping("/assessment")
    public ResponseEntity<EmployeeReviewResponse> saveAssessment(
            @Valid @RequestBody EmployeeAssessmentRequest request) {
        return ResponseEntity.ok(service.saveAssessment(request));
    }

    @PutMapping("/assessment")
    public ResponseEntity<EmployeeReviewResponse> updateReviewerAssessment(
            @RequestParam Long assessorId,
            @RequestParam Long employeeId,
            @RequestParam Long cycleId,
            @Valid @RequestBody ReviewerAssessmentUpdateRequest request) {
        return ResponseEntity.ok(service.updateReviewerAssessment(
                assessorId, employeeId, cycleId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeReviewResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody EmployeeReviewRequest request) {
        return ResponseEntity.ok(service.updateReview(id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<EmployeeReviewResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submitReview(id));
    }
}
