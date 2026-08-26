package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PerformanceCycleRatingScaleRequest {

    private Long cycleId;

    @NotBlank(message = "scaleName is required")
    private String scaleName;

    @NotNull(message = "ratingScaleId is required")
    private Long ratingScaleId;
}
