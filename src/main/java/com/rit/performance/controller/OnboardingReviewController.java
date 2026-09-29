package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hr/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class OnboardingReviewController {
    private final OnboardingReviewService service;

    @GetMapping
    public QueueResponse queue(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = service.queue(status, page, size);
        return new QueueResponse(result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize());
    }

    public record QueueResponse(java.util.List<OnboardingResponse> content, long totalElements,
            int totalPages, int number, int size) {}

    @GetMapping("/{employeeId}")
    public OnboardingResponse review(@PathVariable Long employeeId) {
        return service.review(employeeId);
    }
}
