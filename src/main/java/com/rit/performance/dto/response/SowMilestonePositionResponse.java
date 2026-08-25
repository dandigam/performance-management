package com.rit.performance.dto.response;

import lombok.*;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionResponse {
    private Long milestonePositionId;
    private Long positionId;
    private String positionName;
    private String seniority;
    private Long rateCardId;
    private BigDecimal hourlyRate;
    private String currency;
    private String positionType;
    @JsonProperty("location")
    private String locationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String hours;
    private BigDecimal amount;
    private List<SowResourceAssignmentResponse> assignments;
}
