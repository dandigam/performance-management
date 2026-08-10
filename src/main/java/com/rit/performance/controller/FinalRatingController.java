package com.rit.performance.controller;

import com.rit.performance.dto.FinalRatingResponse;
import com.rit.performance.dto.PublishRatingRequest;
import com.rit.performance.service.FinalRatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/final-ratings")
public class FinalRatingController {

    private final FinalRatingService service;

    public FinalRatingController(FinalRatingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FinalRatingResponse>> getAll() {
        return ResponseEntity.ok(service.getAllFinalRatings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinalRatingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFinalRatingById(id));
    }

    @PostMapping("/{employeeReviewId}/publish")
    public ResponseEntity<FinalRatingResponse> publish(@PathVariable Long employeeReviewId,
            @Valid @RequestBody PublishRatingRequest request) {
        return ResponseEntity.ok(service.publishRating(employeeReviewId, request.getPublishedById()));
    }

    @GetMapping("/my-rating")
    public ResponseEntity<FinalRatingResponse> getMyRating(@RequestParam Long employeeId,
            @RequestParam Long cycleId) {
        return ResponseEntity.ok(service.getMyRating(employeeId, cycleId));
    }
}
