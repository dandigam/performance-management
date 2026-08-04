package com.rit.performance.controller;

import com.rit.performance.dto.CycleDetailsRequest;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.dto.ReviewCycleRequest;
import com.rit.performance.dto.ReviewCycleResponse;
import com.rit.performance.service.CycleDetailsService;
import com.rit.performance.dto.CyclePublishResponse;
import com.rit.performance.service.ReviewCyclePublishService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/review-cycles")
public class ReviewCycleController {

    private final CycleDetailsService service;
    private final ReviewCyclePublishService publishService;

    public ReviewCycleController(CycleDetailsService service, ReviewCyclePublishService publishService) {
        this.service = service;
        this.publishService = publishService;
    }

    @PostMapping("/cycle-details")
    public ResponseEntity<CycleDetailsResponse> createCycleDetails(@Valid @RequestBody ReviewCycleRequest request) {
        boolean isUpdate = request.getCycleDetails().getId() != null;
        CycleDetailsResponse created = service.createCycleDetails(request);
        if (isUpdate) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/api/review-cycles/cycle-details/" + created.getId())).body(created);
    }

    @PutMapping("/cycle-details/{id}")
    public ResponseEntity<CycleDetailsResponse> updateCycleDetails(@PathVariable Long id, @Valid @RequestBody CycleDetailsRequest request) {
        return ResponseEntity.ok(service.updateCycleDetails(id, request));
    }

    @GetMapping("/cycle-details")
    public ResponseEntity<List<CycleDetailsResponse>> getAllCycleDetails() {
        return ResponseEntity.ok(service.getAllCycleDetails());
    }

    @GetMapping("/cycle-details/{id}")
    public ResponseEntity<ReviewCycleResponse> getReviewCycleById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getReviewCycleById(id));
    }

    @GetMapping("/cycle-details/all/full")
    public ResponseEntity<List<ReviewCycleResponse>> getAllReviewCycles() {
        return ResponseEntity.ok(service.getAllReviewCycles());
    }

    @PostMapping("/{cycleId}/publish")
    public ResponseEntity<CyclePublishResponse> publishCycle(
            @PathVariable Long cycleId,
            @RequestHeader(value = "X-Employee-Id", required = false)
            Long publishedBy) {
        return ResponseEntity.ok(publishService.publish(cycleId, publishedBy));
    }
}
