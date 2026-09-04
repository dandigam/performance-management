package com.rit.performance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmployeeAuditHistoryResponse {
    private Long id;
    private Long employeeId;
    private String entityType;
    private String action;
    private Long changedBy;
    private String changedByName;
    private String changedByEmail;
    private LocalDateTime changedOn;
    private Object oldValues;
    private Object newValues;
}
