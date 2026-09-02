package com.rit.performance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowResourceRequirementItemResponse {
    private Long id;
    private Long positionId;
    private String positionName;
    private Long skillId;
    private String skillName;
    private Long seniorityId;
    private String seniority;
    private String location;
    private Integer requiredHc;
}
