package com.rit.performance.service;

import com.rit.performance.dto.SowResourceRequirementResponse;
import com.rit.performance.dto.SowResourceRequirementSummaryResponse;
import com.rit.performance.dto.SowRequirementMilestonesResponse;

import java.util.List;

public interface SowResourceRequirementService {
    void rebuild(Long sowId);
    void onPositionCreatedOrUpdated(Long sowId);
    void onPositionRemoved(Long sowId);
    void onResourceAssigned(Long sowId);
    void onResourceUnassigned(Long sowId);
    void onResourceCompleted(Long sowId);
    void clear(Long sowId);
    List<SowResourceRequirementResponse> getAll();
    List<SowResourceRequirementSummaryResponse> getAllBySow(String sowStatus);
    SowResourceRequirementSummaryResponse getBySowId(Long sowId);
    SowRequirementMilestonesResponse getMilestonesByPosition(
            Long sowId, Long positionId, Long skillId, Long seniorityId, String location);
}
