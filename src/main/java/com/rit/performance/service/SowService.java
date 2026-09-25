package com.rit.performance.service;

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
import com.rit.performance.dto.request.SowSignatureUpdateRequest;
import com.rit.performance.dto.request.SowStatusUpdateRequest;

import java.util.List;

public interface SowService {
    void deleteMilestone(Long sowId, Long milestoneId);

    SowMilestoneResponse updateMilestone(Long sowId, Long milestoneId, SowMilestoneUpdateRequest request);

    SowPositionSummaryPageResponse getPositionSummaries(Long sowId, Long milestoneId, int page, int size);

    SowMilestoneSummaryPageResponse getMilestoneSummaries(Long sowId, int page, int size);

    SowSummaryPageResponse getSummaries(int page, int size);

    SowResponse create(SowRequest request);

    List<SowResponse> getAll();

    List<SowResponse> getAll(Long sowId, String status, Long designationId);

    SowResponse getById(Long id);

    SowRequirementMilestonesResponse getMilestonesByPosition(
            Long sowId, Long positionId, Long skillId, Long seniorityId, String location);

    List<SowAssignmentResponse> getAllAssignments();

    List<SowAssignmentResponse> getAssignments(Long sowId);

    SowAssignmentResponse updateAssignment(Long assignmentId, SowAssignmentUpdateRequest request);

    SowMilestonePositionResponse createPosition(
            Long sowId, Long milestoneId, SowMilestonePositionRequest request);

    void deletePosition(Long sowId, Long milestoneId, Long positionId);

    SowAssignmentResponse unassignFromSow(
            Long sowId, Long assignmentId, SowAssignmentUnassignRequest request);

    SowResponse update(Long id, SowRequest request);

    SowResponse updateStatus(Long sowId, SowStatusUpdateRequest request);

    SowResponse updateSignature(Long sowId, SowSignatureUpdateRequest request);

    void delete(Long id);
}
