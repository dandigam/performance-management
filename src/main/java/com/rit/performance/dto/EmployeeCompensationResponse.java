package com.rit.performance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCompensationResponse {
    private Long id;
    private String payType;
    private BigDecimal hourlyRate;
    private String currency;
    private LocalDate effectiveDate;
}
