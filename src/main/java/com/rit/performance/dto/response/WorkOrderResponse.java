package com.rit.performance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.rit.performance.dto.DocumentResponse;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrderResponse {
    private Long id;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private Long sowId;
    private String sowCode;
    private String sowName;
    private BigDecimal amount;
    private BigDecimal hourlyRate;
    private BigDecimal salary;
    private BigDecimal commission;
    private Long employeeId;
    private String employeeName;
    private String comments;
    private List<DocumentResponse> documentList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
