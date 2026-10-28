package com.rit.performance.controller;

import com.rit.performance.dto.*;
import com.rit.performance.service.FeatureFlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FeatureFlagController {
    private final FeatureFlagService service;

    @GetMapping("/features")
    public List<FeatureFlagService.FeatureResponse> list() { return service.list(); }

    @GetMapping("/features/{id}")
    public FeatureFlagService.FeatureResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/features")
    public ResponseEntity<FeatureFlagService.FeatureResponse> create(@Valid @RequestBody FeatureRequest request) {
        var feature = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/features/" + feature.id())).body(feature);
    }

    @PutMapping("/features/{id}")
    public FeatureFlagService.FeatureResponse update(@PathVariable Long id, @Valid @RequestBody FeatureRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/roles/{roleId}/features")
    public List<FeatureFlagService.RoleFeatureResponse> forRole(@PathVariable Long roleId) { return service.forRole(roleId); }

    @PutMapping("/roles/{roleId}/features/{featureId}")
    public FeatureFlagService.RoleFeatureResponse set(@PathVariable Long roleId, @PathVariable Long featureId,
            @Valid @RequestBody RoleFeatureFlagRequest request) {
        return service.set(roleId, featureId, request.enabled());
    }
}
