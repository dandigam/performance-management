package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleDetailsResponse {

    private Long id;
    private String cycleName;
    private LocalDate evaluationStartDate;
    private LocalDate evaluationEndDate;
    private String description;
    private Long reviewTypeId;
    private String reviewTypeName;
    private Long applicableTypeId;
    private List<Long> scopeValueIds;
    private String status;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
