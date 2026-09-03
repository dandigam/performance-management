package com.rit.performance.controller;

import com.rit.performance.dto.SowResourceRequirementResponse;
import com.rit.performance.dto.SowResourceRequirementSummaryResponse;
import com.rit.performance.service.SowResourceRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sow-resource-requirements")
@RequiredArgsConstructor
public class SowResourceRequirementController {
    private final SowResourceRequirementService service;

    @GetMapping
    public ResponseEntity<List<SowResourceRequirementResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/sows")
    public ResponseEntity<List<SowResourceRequirementSummaryResponse>> getAllBySow(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getAllBySow(status));
    }

    @GetMapping("/sows/{sowId}")
    public ResponseEntity<SowResourceRequirementSummaryResponse> getBySowId(
            @PathVariable Long sowId) {
        return ResponseEntity.ok(service.getBySowId(sowId));
    }

}
