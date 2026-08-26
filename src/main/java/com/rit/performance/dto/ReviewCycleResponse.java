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
public class ReviewCycleResponse {

    private CycleDetailsResponse cycleDetails;

    private List<PerformanceCycleAssessorResponse> assessors;

    private List<PerformanceCycleTimelineResponse> timelinePhases;

    private AssessmentSetupResponse assessmentSetup;

    private PerformanceCycleRatingScaleResponse ratingScale;
}
