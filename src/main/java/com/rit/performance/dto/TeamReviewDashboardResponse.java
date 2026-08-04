package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamReviewDashboardResponse {
    private Long cycleId;
    private String cycleName;
    private Long assessorEmployeeId;
    private String assessorEmployeeName;
    private List<TeamReviewMemberResponse> teamMembers;
}
