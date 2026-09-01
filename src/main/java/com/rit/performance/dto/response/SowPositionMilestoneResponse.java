package com.rit.performance.dto.response;

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
public class SowPositionMilestoneResponse {
    private Long milestoneId;
    private String milestoneName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
