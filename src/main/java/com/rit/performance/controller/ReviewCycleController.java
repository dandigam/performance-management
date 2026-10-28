package com.rit.performance.controller;

import com.rit.performance.dto.CycleDetailsRequest;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.dto.ReviewCycleRequest;
import com.rit.performance.dto.ReviewCycleResponse;
import com.rit.performance.service.CycleDetailsService;
import com.rit.performance.dto.CyclePublishResponse;
import com.rit.performance.dto.CyclePublishRequest;
import com.rit.performance.service.ReviewCyclePublishService;
import com.rit.performance.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/performance-cycles")
@RequiredArgsConstructor
public class ReviewCycleController {

    private final CycleDetailsService service;
    private final ReviewCyclePublishService publishService;

    @PostMapping
    public ResponseEntity<CycleDetailsResponse> createCycleDetails(@Valid @RequestBody ReviewCycleRequest request) {
        boolean isUpdate = request.getCycleDetails().getId() != null;
        CycleDetailsResponse created = service.createCycleDetails(request);
        if (isUpdate) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/api/performance-cycles/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CycleDetailsResponse> updateCycleDetails(@PathVariable Long id, @Valid @RequestBody CycleDetailsRequest request) {
        return ResponseEntity.ok(service.updateCycleDetails(id, request));
    }

    @GetMapping
    public ResponseEntity<List<CycleDetailsResponse>> getAllCycleDetails() {
        return ResponseEntity.ok(service.getAllCycleDetails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewCycleResponse> getReviewCycleById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getReviewCycleById(id));
    }

    @GetMapping("/all/full")
    public ResponseEntity<List<ReviewCycleResponse>> getAllReviewCycles() {
        return ResponseEntity.ok(service.getAllReviewCycles());
    }

    @PostMapping("/{cycleId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<CyclePublishResponse> publishCycle(@PathVariable Long cycleId, @AuthenticationPrincipal AuthenticatedUser publisher,
                                                             @Valid @RequestBody(required = false) CyclePublishRequest request) {
        return ResponseEntity.ok(publishService.publish(cycleId, publisher.id(), request));
    }
}
