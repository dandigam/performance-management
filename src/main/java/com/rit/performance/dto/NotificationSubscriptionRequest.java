package com.rit.performance.dto;

import com.rit.performance.entity.NotificationSubscription.RecipientType;
import jakarta.validation.constraints.*;

public record NotificationSubscriptionRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 10000) String emailAddresses,
        @NotNull RecipientType recipientType,
        @NotNull Boolean active) {}
