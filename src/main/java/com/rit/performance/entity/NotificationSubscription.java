package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_subscriptions", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_subscription_category", columnNames = "category_id"))
@Getter
@Setter
@NoArgsConstructor
public class NotificationSubscription extends BaseEntity {
    public enum RecipientType { CC, BCC }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private LookupValue category;

    @Column(name = "email_addresses", nullable = false, length = 10000)
    private String emailAddresses;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 3)
    private RecipientType recipientType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
