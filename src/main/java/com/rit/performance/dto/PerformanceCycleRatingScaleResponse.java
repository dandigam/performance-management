package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceCycleRatingScaleResponse {

    private Long id;

    private Long cycleId;

    private String scaleName;

    private Long ratingScaleId;

    private Boolean active;
}
