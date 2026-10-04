package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_notification_event", columnNames = {"recipient_user_id", "deduplication_key"}), indexes = {
        @Index(name = "idx_user_notification_list", columnList = "recipient_user_id,created_on,id"),
        @Index(name = "idx_user_notification_unread", columnList = "recipient_user_id,read_at")})
@Getter @Setter @NoArgsConstructor
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(name = "related_record_type", nullable = false, length = 40)
    private String relatedRecordType;
    @Column(name = "related_record_id", nullable = false)
    private Long relatedRecordId;
    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;
}
