package com.rit.performance.service;

import com.rit.performance.entity.PasswordResetToken;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    public static final String INVALID_LINK = "This reset link is invalid or expired. Please request a new one.";
    public static final String EMAIL_NOT_FOUND = "No active account was found for this email address.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final PasswordResetRateLimiter limiter;
    private final ApplicationEventPublisher events;
    private final EmailNotificationService notifications;
    private final Clock clock;
    @Value("${app.mail.base-url:http://localhost:5173}") private String frontendUrl;

    @Transactional
    public void forgot(String email, String remoteAddress) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!limiter.allow("forgot-ip:" + remoteAddress, 20, Duration.ofHours(1))) throw new com.rit.performance.exception.PasswordResetRateLimitException();
        if (!limiter.allow("forgot-email:" + normalized, 3, Duration.ofHours(1))) throw new com.rit.performance.exception.PasswordResetRateLimitException();
        var id = users.findActiveIdByEmail(normalized);
        if (id.isEmpty()) throw new InvalidOperationException(EMAIL_NOT_FOUND);
        var user = users.findForSecurityUpdate(id.get()).orElse(null);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus()) || user.getEmployee() == null) throw new InvalidOperationException(EMAIL_NOT_FOUND);
        // Recheck the address after acquiring the account lock.
        if (!normalized.equalsIgnoreCase(user.getEmployee().getEmail())) throw new InvalidOperationException(EMAIL_NOT_FOUND);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(PasswordResetRateLimiter.hash(raw));
        token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(30)));
        tokens.save(token);
        events.publishEvent(new PasswordResetEmail(user.getEmployee().getEmail(),
                frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + raw));
    }

    @Transactional
    public void reset(String raw, String password) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String hash = PasswordResetRateLimiter.hash(raw);
        Long id = tokens.findUserIdByHash(hash).orElseThrow(PasswordResetService::invalid);
        // All reset, login and refresh flows acquire the user lock before token locks.
        var user = users.findForSecurityUpdate(id).orElseThrow(PasswordResetService::invalid);
        var token = tokens.findByTokenHash(hash).orElseThrow(PasswordResetService::invalid);
        if (token.isUsed() || !token.getExpiresAt().isAfter(clock.instant())
                || !"ACTIVE".equalsIgnoreCase(user.getStatus())) throw invalid();
        PasswordPolicy.validate(password);
        if (encoder.matches(password, user.getPassword()))
            throw new InvalidOperationException("New password must be different from the current password.");
        user.setPassword(encoder.encode(password));
        user.setSessionVersion(user.getSessionVersion() + 1);
        users.save(user);
        tokens.invalidateAllForUser(id);
        refreshTokens.revokeAllForUser(id, clock.instant());
        notifications.queuePasswordChanged(user);
    }

    private static InvalidOperationException invalid() { return new InvalidOperationException(INVALID_LINK); }
}
