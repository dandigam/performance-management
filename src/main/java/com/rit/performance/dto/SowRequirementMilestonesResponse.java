package com.rit.performance.dto;

import com.rit.performance.dto.response.SowPositionMilestoneResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowRequirementMilestonesResponse {
    private Long sowId;
    private Long requirementId;
    private Long positionId;
    private String positionName;
    private Long skillId;
    private String skillName;
    private String seniority;
    private String location;
    private List<SowPositionMilestoneResponse> milestones;
}
