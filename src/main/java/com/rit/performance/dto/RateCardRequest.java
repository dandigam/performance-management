package com.rit.performance.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

@Getter
@Setter
public class RateCardRequest {
    private Long positionTitleId;
    @NotNull(message = "mainSkillId is required")
    private Long mainSkillId;
    @JsonAlias("skill")
    @Size(max = 1000, message = "additionalSkills must not exceed 1000 characters")
    private String additionalSkills;
    private Long locationId;
    private Long seniorityId;
    private Long clientId;
    private BigDecimal hourlyRate;
    private String currency = "USD";
    @NotNull(message = "effectiveFrom is required")
    @JsonAlias("effectiveDate")
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
}
