package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceCycleAssessorResponse {

    private Long id;

    private Long roleId;

    private String assessorName;

    private Long actionTypeId;

    private BigDecimal weightage;

    private Integer displayOrder;
}
