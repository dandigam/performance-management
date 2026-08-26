package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class ReviewCycleRequest {

    @Valid
    @NotNull(message = "cycleDetails is required")
    private CycleDetailsRequest cycleDetails;

    private List<@Valid PerformanceCycleAssessorRequest> assessors;

    private List<@Valid PerformanceCycleTimelineRequest> timelinePhases;

    @Valid
    private AssessmentSetupRequest assessmentSetup;

    @Valid
    private PerformanceCycleRatingScaleRequest ratingScale;
}
