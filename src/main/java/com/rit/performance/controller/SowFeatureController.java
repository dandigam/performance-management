package com.rit.performance.controller;

import com.rit.performance.dto.request.SowFeatureRequest;
import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.dto.response.SowProgressSummaryResponse;
import com.rit.performance.service.SowFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sows/{sowId}")
@RequiredArgsConstructor
public class SowFeatureController {
    private final SowFeatureService featureService;

    @PostMapping("/features")
    public ResponseEntity<SowFeatureResponse> create(
            @PathVariable Long sowId,
            @Valid @RequestBody SowFeatureRequest request
    ) {
        SowFeatureResponse created = featureService.create(sowId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/sows/" + sowId + "/features/" + created.getId()
        )).body(created);
    }

    @GetMapping("/features")
    public ResponseEntity<List<SowFeatureResponse>> getAll(@PathVariable Long sowId) {
        return ResponseEntity.ok(featureService.getAll(sowId));
    }

    @GetMapping("/features/{featureId}")
    public ResponseEntity<SowFeatureResponse> getById(
            @PathVariable Long sowId,
            @PathVariable Long featureId
    ) {
        return ResponseEntity.ok(featureService.getById(sowId, featureId));
    }

    @PutMapping("/features/{featureId}")
    public ResponseEntity<SowFeatureResponse> update(
            @PathVariable Long sowId,
            @PathVariable Long featureId,
            @Valid @RequestBody SowFeatureRequest request
    ) {
        return ResponseEntity.ok(featureService.update(sowId, featureId, request));
    }

    @DeleteMapping("/features/{featureId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long sowId,
            @PathVariable Long featureId
    ) {
        featureService.delete(sowId, featureId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/progress")
    public ResponseEntity<SowProgressSummaryResponse> getProgress(@PathVariable Long sowId) {
        return ResponseEntity.ok(featureService.getProgress(sowId));
    }
}
