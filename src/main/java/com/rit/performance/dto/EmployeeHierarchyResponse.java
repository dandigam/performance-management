package com.rit.performance.dto;

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
public class EmployeeHierarchyResponse {
    private Long viewerEmployeeId;
    private String viewerEmployeeName;
    private String roleType;
    private Long cycleId;
    private String cycleName;
    @Builder.Default
    private List<EmployeeHierarchyMemberResponse> employees = List.of();
}
