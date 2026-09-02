package com.rit.performance.dto.response;

import lombok.*;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionResponse {
    private Long milestonePositionId;
    private Long positionId;
    private String positionName;
    private Long skillId;
    private String skillName;
    private Long seniorityId;
    private String seniority;
    private Long rateCardId;
    private BigDecimal hourlyRate;
    private String rateOverrideReason;
    private Long rateUpdatedBy;
    private LocalDateTime rateUpdatedDate;
    private String currency;
    private String positionType;
    private String status;
    @JsonProperty("location")
    private String locationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String hours;
    private BigDecimal amount;
    private List<SowResourceAssignmentResponse> assignments;
}
