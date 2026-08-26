package com.rit.performance.controller;

import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.request.SowAssignmentUpdateRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.dto.response.SowAssignmentResponse;
import com.rit.performance.dto.request.SowMilestonePositionRequest;
import com.rit.performance.dto.response.SowMilestonePositionResponse;
import com.rit.performance.dto.request.SowAssignmentUnassignRequest;
import com.rit.performance.dto.request.SowMilestonePositionAssignmentRequest;
import com.rit.performance.dto.request.SowMilestonePositionUnassignRequest;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;
import com.rit.performance.service.SowService;
import com.rit.performance.service.SowMilestonePositionAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sows")
@RequiredArgsConstructor
public class SowController {
    private final SowService sowService;
    private final SowMilestonePositionAssignmentService positionAssignmentService;

    @PostMapping
    public ResponseEntity<SowResponse> create(@Valid @RequestBody SowRequest request) {
        SowResponse created = sowService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sows/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<SowResponse>> getAll(
            @RequestParam(required = false) Long sowId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long designationId) {
        return ResponseEntity.ok(sowService.getAll(sowId, status, designationId));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<SowAssignmentResponse>> getAllAssignments() {
        return ResponseEntity.ok(sowService.getAllAssignments());
    }

    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<SowAssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SowAssignmentUpdateRequest request) {
        return ResponseEntity.ok(sowService.updateAssignment(assignmentId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SowResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sowService.getById(id));
    }

    @GetMapping("/{sowId}/assignments")
    public ResponseEntity<List<SowAssignmentResponse>> getAssignments(@PathVariable Long sowId) {
        return ResponseEntity.ok(sowService.getAssignments(sowId));
    }

    @PostMapping("/{sowId}/milestones/{milestoneId}/positions")
    public ResponseEntity<SowMilestonePositionResponse> createPosition(
            @PathVariable Long sowId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody SowMilestonePositionRequest request) {
        SowMilestonePositionResponse created =
                sowService.createPosition(sowId, milestoneId, request);
        return ResponseEntity.created(URI.create("/api/v1/sows/" + sowId
                + "/milestones/" + milestoneId + "/positions/"
                + created.getMilestonePositionId())).body(created);
    }

    @PostMapping("/{sowId}/milestones/{milestoneId}/positions/"
            + "{milestonePositionId}/assignments")
    public ResponseEntity<SowMilestonePositionAssignmentResponse> assignToPosition(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @PathVariable Long milestonePositionId,
            @Valid @RequestBody SowMilestonePositionAssignmentRequest request) {
        SowMilestonePositionAssignmentResponse created = positionAssignmentService.create(
                sowId, milestoneId, milestonePositionId, request);
        return ResponseEntity.created(URI.create("/api/v1/sows/" + sowId
                + "/milestones/" + milestoneId + "/positions/" + milestonePositionId
                + "/assignments/" + created.getId())).body(created);
    }

    @GetMapping("/{sowId}/milestones/{milestoneId}/positions/"
            + "{milestonePositionId}/assignments")
    public ResponseEntity<List<SowMilestonePositionAssignmentResponse>> getPositionAssignments(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @PathVariable Long milestonePositionId) {
        return ResponseEntity.ok(positionAssignmentService.getAll(
                sowId, milestoneId, milestonePositionId));
    }

    @PutMapping("/{sowId}/milestones/{milestoneId}/positions/"
            + "{milestonePositionId}/assignments/{assignmentId}")
    public ResponseEntity<SowMilestonePositionAssignmentResponse> updatePositionAssignment(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @PathVariable Long milestonePositionId, @PathVariable Long assignmentId,
            @Valid @RequestBody SowMilestonePositionAssignmentRequest request) {
        return ResponseEntity.ok(positionAssignmentService.update(sowId, milestoneId,
                milestonePositionId, assignmentId, request));
    }

    @PatchMapping("/{sowId}/milestones/{milestoneId}/positions/"
            + "{milestonePositionId}/assignments/{assignmentId}/unassign")
    public ResponseEntity<SowMilestonePositionAssignmentResponse> unassignFromPosition(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @PathVariable Long milestonePositionId, @PathVariable Long assignmentId,
            @Valid @RequestBody SowMilestonePositionUnassignRequest request) {
        return ResponseEntity.ok(positionAssignmentService.unassign(sowId, milestoneId,
                milestonePositionId, assignmentId, request));
    }

    @PatchMapping("/{sowId}/assignments/{assignmentId}/unassign")
    public ResponseEntity<SowMilestonePositionAssignmentResponse> unassignFromSow(
            @PathVariable Long sowId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody SowAssignmentUnassignRequest request) {
        return ResponseEntity.ok(positionAssignmentService.unassign(
                sowId, assignmentId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SowResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody SowRequest request) {
        return ResponseEntity.ok(sowService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
