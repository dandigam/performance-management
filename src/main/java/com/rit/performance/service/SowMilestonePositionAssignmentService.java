package com.rit.performance.service;

import com.rit.performance.dto.request.SowMilestonePositionAssignmentRequest;
import com.rit.performance.dto.request.SowMilestonePositionUnassignRequest;
import com.rit.performance.dto.request.SowAssignmentUnassignRequest;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;
import java.util.List;

public interface SowMilestonePositionAssignmentService {
    SowMilestonePositionAssignmentResponse create(Long sowId, Long milestoneId,
            Long milestonePositionId, SowMilestonePositionAssignmentRequest request);
    List<SowMilestonePositionAssignmentResponse> getAll(Long sowId, Long milestoneId,
            Long milestonePositionId);
    List<SowMilestonePositionAssignmentResponse> getByEmployeeId(Long employeeId);
    SowMilestonePositionAssignmentResponse update(Long sowId, Long milestoneId,
            Long milestonePositionId, Long id,
            SowMilestonePositionAssignmentRequest request);
    SowMilestonePositionAssignmentResponse unassign(Long sowId, Long milestoneId,
            Long milestonePositionId, Long id,
            SowMilestonePositionUnassignRequest request);
    SowMilestonePositionAssignmentResponse unassign(Long sowId, Long id,
            SowAssignmentUnassignRequest request);
}
