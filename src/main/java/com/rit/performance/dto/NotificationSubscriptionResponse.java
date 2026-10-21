package com.rit.performance.dto;

import com.rit.performance.entity.NotificationSubscription.RecipientType;
import java.time.LocalDateTime;

public record NotificationSubscriptionResponse(
        Long id, Long categoryId, String categoryCode, String categoryName,
        String emailAddresses, RecipientType recipientType, boolean active,
        Long createdBy, LocalDateTime createdOn, Long updatedBy, LocalDateTime updatedOn) {}
