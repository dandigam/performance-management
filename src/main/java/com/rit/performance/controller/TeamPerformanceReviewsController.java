package com.rit.performance.controller;

import com.rit.performance.dto.TeamPerformanceReviewsResponse;
import com.rit.performance.service.TeamPerformanceReviewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TeamPerformanceReviewsController {
    private final TeamPerformanceReviewsService service;

    @GetMapping("/api/employee-reviews/me/team-performance-reviews")
    public TeamPerformanceReviewsResponse getMyTeamReviews(@RequestParam Long cycleId) {
        return service.getMyTeamReviews(cycleId);
    }
}
