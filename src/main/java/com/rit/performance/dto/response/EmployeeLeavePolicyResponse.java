package com.rit.performance.dto.response;

import com.rit.performance.entity.LeavePolicyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeLeavePolicyResponse(
        Long id, Long employeeId, Long leavePolicyId, String policyName,
        LocalDate effectiveFrom, LocalDate effectiveTo, LeavePolicyStatus status,
        LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {}
