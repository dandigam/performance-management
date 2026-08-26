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
public class ReviewProgressSummaryResponse {
    private long totalEmployees;
    private long selfReviewsCompleted;
    private long teamLeadReviewsCompleted;
    private long managerReviewsCompleted;
    private long ratingsPublished;
}
