package com.rit.performance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeFinanceHistoryResponse {
    private Long id;
    private String payType;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String currency;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private boolean current;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
