package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceCycleTimelineResponse {

    private Long id;

    private String phaseName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long roleId;

    private Integer displayOrder;
}
