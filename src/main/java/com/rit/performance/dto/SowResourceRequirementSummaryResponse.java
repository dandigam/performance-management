package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowResourceRequirementSummaryResponse {
    private Long sowId;
    private String sowCode;
    private String sowName;
    private Long businessUnitId;
    private String businessUnitName;
    private Long projectOwnerEmployeeId;
    private String projectOwnerEmployeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalRequiredHc;
    private List<SowResourceRequirementItemResponse> positionInfo;
}
