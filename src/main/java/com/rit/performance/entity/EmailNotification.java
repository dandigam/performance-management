package com.rit.performance.entity;

import com.rit.performance.service.EmailDeliveryStatus;
import com.rit.performance.service.EmailEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_email_notification_deduplication", columnNames = "deduplication_key"), indexes = {
        @Index(name = "idx_email_notification_status", columnList = "status,next_attempt_date"),
        @Index(name = "idx_email_notification_recipient", columnList = "recipient_email"),
        @Index(name = "idx_email_notification_cycle", columnList = "cycle_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EmailEventType eventType;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 500)
    private String footer;

    @Column(name = "action_url", length = 1000)
    private String actionUrl;

    @Column(name = "employee_review_id")
    private Long employeeReviewId;

    @Column(name = "cycle_id")
    private Long cycleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmailDeliveryStatus status = EmailDeliveryStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "next_attempt_date")
    private LocalDateTime nextAttemptDate;

    @PrePersist
    void prePersist() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (status == null) status = EmailDeliveryStatus.PENDING;
    }
}
