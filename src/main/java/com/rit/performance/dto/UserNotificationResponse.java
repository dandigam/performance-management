package com.rit.performance.dto;

import java.time.Instant;

public record UserNotificationResponse(Long id, String category, String eventType, String title,
        String message, Instant createdOn, boolean read, Instant readAt,
        String relatedRecordType, Long relatedRecordId) {}
