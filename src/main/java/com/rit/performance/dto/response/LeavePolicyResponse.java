package com.rit.performance.dto.response;

import com.rit.performance.entity.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LeavePolicyResponse(Long id, String policyName, LeavePolicyCountry country,
        EmploymentType employmentType, LocalDate effectiveFrom, LocalDate effectiveTo,
        LeavePolicyStatus status, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt,
        Long updatedBy, List<LeavePolicyRuleResponse> rules) {}
