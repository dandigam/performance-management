package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "idx_password_reset_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean used;
    @Column(name = "otp_hash", length = 100)
    private String otpHash;
    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;
    @Column(name = "otp_sent_at")
    private Instant otpSentAt;
    @Column(name = "otp_attempts", nullable = false, columnDefinition = "int default 0")
    private int otpAttempts;
    @Column(name = "otp_send_count", nullable = false, columnDefinition = "int default 0")
    private int otpSendCount;
}
