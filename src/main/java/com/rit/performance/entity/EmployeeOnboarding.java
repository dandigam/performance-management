package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "employee_onboardings", uniqueConstraints = @UniqueConstraint(
        name = "uk_onboarding_request", columnNames = {"request_actor", "idempotency_key"}))
@Getter
@Setter
public class EmployeeOnboarding extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;
    @Column(name = "request_actor", nullable = false, length = 100)
    private String requestActor;
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(nullable = false, length = 30)
    private String status = "INVITED";
    @Column(name = "required_sections", nullable = false, columnDefinition = "TEXT")
    private String requiredSections;
    @Column(name = "original_response", columnDefinition = "TEXT")
    private String originalResponse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invitation_token_id")
    private PasswordResetToken invitationToken;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invitation_notification_id")
    private EmailNotification invitationNotification;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;
    @Column(name = "reviewed_by", length = 150)
    private String reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Version
    private long version;
}
