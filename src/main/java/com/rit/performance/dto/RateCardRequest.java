package com.rit.performance.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

@Getter
@Setter
public class RateCardRequest {
    private Long positionTitleId;
    @NotBlank(message = "skill is required")
    @Size(max = 100, message = "skill must not exceed 100 characters")
    private String skill;
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
