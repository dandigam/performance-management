package com.rit.performance.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowResourceRequirementResponse {
    private Long id;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long positionId;
    private String positionName;
    private Long skillId;
    private String skillName;
    private String seniority;
    private String location;
    private Integer requiredHc;
}
