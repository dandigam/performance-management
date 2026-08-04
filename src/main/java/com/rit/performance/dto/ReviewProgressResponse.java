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
public class ReviewProgressResponse {
    private Long cycleId;
    private String cycleName;
    private String cycleStatus;
    private ReviewProgressSummaryResponse summary;
    @Builder.Default
    private List<ReviewProgressEmployeeResponse> employees = List.of();
}
