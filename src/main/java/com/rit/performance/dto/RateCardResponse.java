package com.rit.performance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RateCardResponse {
    private Long id;
    private Long positionTitleId;
    private String positionTitleName;
    private String skill;
    private Long locationId;
    private String locationName;
    private Long seniorityId;
    private String seniorityName;
    private Long clientId;
    private String clientName;
    private BigDecimal hourlyRate;
    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private LocalDateTime createdDate;
}
