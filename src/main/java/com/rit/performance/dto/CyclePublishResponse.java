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
public class CyclePublishResponse {
    private Long cycleId;
    private String cycleStatus;
    private int eligibleEmployees;
    private int selectedEmployees;
    private int excludedEmployees;
    private int alreadySubmittedEmployees;
    private int emailsScheduled;
    private int bellNotificationsScheduled;
}
