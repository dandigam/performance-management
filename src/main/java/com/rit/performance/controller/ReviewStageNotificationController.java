package com.rit.performance.controller;

import com.rit.performance.service.ReviewStageNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/employee-reviews/{reviewId}/stages/{stageId}/notification")
public class ReviewStageNotificationController {
    private final ReviewStageNotificationService service;

    public record ResendRequest(@NotBlank @Email @Size(max = 150) String expectedEmail) {}

    @GetMapping
    public ReviewStageNotificationService.NotificationInfo get(@PathVariable Long reviewId, @PathVariable Long stageId) {
        return service.get(reviewId, stageId);
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resend(@PathVariable Long reviewId, @PathVariable Long stageId,
                                      @Valid @RequestBody ResendRequest request) {
        service.resend(reviewId, stageId, request.expectedEmail());
        return ResponseEntity.accepted().build();
    }
}
