package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestoneResponse {
    private Long id;
    private String milestoneName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private String status;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
