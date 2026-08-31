package com.rit.performance.dto;

import java.time.LocalDateTime;

/** Reusable audit fields for API responses. */
public record AuditResponse(
        Long createdBy,
        String createdByName,
        LocalDateTime createdOn,
        Long updatedBy,
        String updatedByName,
        LocalDateTime updatedOn) {
}
