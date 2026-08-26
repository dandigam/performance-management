package com.rit.performance.dto;

import com.rit.performance.service.EmailDeliveryStatus;
import com.rit.performance.service.EmailEventType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationResponse {
    private Long id;
    private EmailEventType eventType;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String body;
    private String footer;
    private String actionUrl;
    private Long employeeReviewId;
    private Long cycleId;
    private EmailDeliveryStatus status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime createdDate;
    private LocalDateTime sentDate;
    private LocalDateTime nextAttemptDate;
}
