package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestoneResponse {
    private Long id;
    private String milestoneName;
    private String description;
    private Integer estimatedHours;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate invoiceDate;
    @JsonProperty("invoiceAmount")
    private BigDecimal amount;
    private String status;
    private List<SowMilestonePositionResponse> positions;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
