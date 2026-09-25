package com.rit.performance.controller;

import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.request.SowMilestoneUpdateRequest;
import com.rit.performance.dto.response.SowMilestoneResponse;
import com.rit.performance.dto.request.SowAssignmentUpdateRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.dto.response.SowSummaryPageResponse;
import com.rit.performance.dto.response.SowPositionSummaryPageResponse;
import com.rit.performance.dto.response.SowMilestoneSummaryPageResponse;
import com.rit.performance.dto.response.SowAssignmentResponse;
import com.rit.performance.dto.request.SowMilestonePositionRequest;
import com.rit.performance.dto.response.SowMilestonePositionResponse;
import com.rit.performance.dto.SowRequirementMilestonesResponse;
import com.rit.performance.dto.request.SowAssignmentUnassignRequest;
import com.rit.performance.dto.request.SowMilestonePositionAssignmentRequest;
import com.rit.performance.dto.request.SowMilestonePositionUnassignRequest;
import com.rit.performance.dto.request.SowSignatureUpdateRequest;
import com.rit.performance.dto.request.SowStatusUpdateRequest;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;
import com.rit.performance.dto.ApiMessageResponse;
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

    @DeleteMapping("/{sowId}/milestones/{milestoneId}")
    public ResponseEntity<ApiMessageResponse> deleteMilestone(
            @PathVariable Long sowId, @PathVariable Long milestoneId) {
        sowService.deleteMilestone(sowId, milestoneId);
        return ResponseEntity.ok(ApiMessageResponse.success("Milestone deleted successfully."));
    }

    @PutMapping("/{sowId}/milestones/{milestoneId}")
    public ResponseEntity<SowMilestoneResponse> updateMilestone(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @Valid @RequestBody SowMilestoneUpdateRequest request) {
        return ResponseEntity.ok(sowService.updateMilestone(sowId, milestoneId, request));
    }

    @GetMapping("/{sowId}/milestones/{milestoneId}/positions/summaries")
    public ResponseEntity<SowPositionSummaryPageResponse> getPositionSummaries(
            @PathVariable Long sowId, @PathVariable Long milestoneId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sowService.getPositionSummaries(sowId, milestoneId, page, size));
    }

    @GetMapping("/{sowId}/milestones/summaries")
    public ResponseEntity<SowMilestoneSummaryPageResponse> getMilestoneSummaries(
            @PathVariable Long sowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sowService.getMilestoneSummaries(sowId, page, size));
    }

    @GetMapping("/summaries")
    public ResponseEntity<SowSummaryPageResponse> getSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sowService.getSummaries(page, size));
    }

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

    @GetMapping("/{sowId}/milestones")
    public ResponseEntity<SowRequirementMilestonesResponse> getMilestonesByPosition(
            @PathVariable Long sowId,
            @RequestParam Long positionId,
            @RequestParam Long skillId,
            @RequestParam Long seniorityId,
            @RequestParam String location) {
        return ResponseEntity.ok(sowService.getMilestonesByPosition(
                sowId, positionId, skillId, seniorityId, location));
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

    @DeleteMapping("/{sowId}/milestones/{milestoneId}/positions/{positionId}")
    public ResponseEntity<ApiMessageResponse> deletePosition(
            @PathVariable Long sowId,
            @PathVariable Long milestoneId,
            @PathVariable Long positionId) {
        sowService.deletePosition(sowId, milestoneId, positionId);
        return ResponseEntity.ok(ApiMessageResponse.success("Position removed successfully."));
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

    @PatchMapping("/{sowId}/status")
    public ResponseEntity<SowResponse> updateStatus(
            @PathVariable Long sowId,
            @Valid @RequestBody SowStatusUpdateRequest request) {
        return ResponseEntity.ok(sowService.updateStatus(sowId, request));
    }

    @PatchMapping("/{sowId}/signature")
    public ResponseEntity<SowResponse> updateSignature(
            @PathVariable Long sowId,
            @Valid @RequestBody SowSignatureUpdateRequest request) {
        return ResponseEntity.ok(sowService.updateSignature(sowId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
