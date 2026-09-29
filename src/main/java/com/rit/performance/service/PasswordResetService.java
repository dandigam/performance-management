package com.rit.performance.service;

import com.rit.performance.entity.PasswordResetToken;
import com.rit.performance.entity.User;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

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
    private final ApplicationEmailFactory emailFactory;
    private final EmailNotificationService notifications;
    private final Clock clock;
    @Value("${app.mail.base-url:http://localhost:5173}") private String frontendUrl;

    @Transactional
    public void forgot(String email, String remoteAddress) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!limiter.allow("forgot-ip:" + remoteAddress, 20, Duration.ofHours(1))) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many password reset requests. Please try again later.", "3600");
        }
        if (!limiter.allow("forgot-email:" + normalized, 3, Duration.ofHours(1))) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many password reset requests. Please try again later.", "3600");
        }

        Optional<Long> id = users.findActiveIdByEmail(normalized);
        if (id.isEmpty()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_FOUND", EMAIL_NOT_FOUND);
        }

        User user = users.findForSecurityUpdate(id.get()).orElse(null);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus()) || user.getEmployee() == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_FOUND", EMAIL_NOT_FOUND);
        }

        // Recheck the address after acquiring the account lock.
        if (!normalized.equalsIgnoreCase(user.getEmployee().getEmail())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_FOUND", EMAIL_NOT_FOUND);
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(PasswordResetRateLimiter.hash(raw));
        token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(30)));
        tokens.save(token);

        String resetLink = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + raw;
        events.publishEvent(emailFactory.passwordReset(user.getEmployee().getEmail(), resetLink));
    }

    @Transactional
    public void reset(String raw, String password) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST,
                    "INVALID_PASSWORD_RESET_LINK", INVALID_LINK);
        }

        String hash = PasswordResetRateLimiter.hash(raw);
        Long id = tokens.findUserIdByHash(hash).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST,
                        "INVALID_PASSWORD_RESET_LINK", INVALID_LINK));

        // All reset, login and refresh flows acquire the user lock before token locks.
        User user = users.findForSecurityUpdate(id).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST,
                        "INVALID_PASSWORD_RESET_LINK", INVALID_LINK));
        PasswordResetToken token = tokens.findByTokenHash(hash).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST,
                        "INVALID_PASSWORD_RESET_LINK", INVALID_LINK));

        boolean accountCanSetPassword = "ACTIVE".equalsIgnoreCase(user.getStatus())
                || "INVITED".equalsIgnoreCase(user.getStatus());
        if (token.isUsed() || !token.getExpiresAt().isAfter(clock.instant())
                || !accountCanSetPassword) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST,
                    "INVALID_PASSWORD_RESET_LINK", INVALID_LINK);
        }

        PasswordPolicy.validate(password);
        if (encoder.matches(password, user.getPassword())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "PASSWORD_REUSE_NOT_ALLOWED",
                    "New password must be different from the current password.");
        }

        user.setPassword(encoder.encode(password));
        if ("INVITED".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        user.setSessionVersion(user.getSessionVersion() + 1);
        users.save(user);

        tokens.invalidateAllForUser(id);
        refreshTokens.revokeAllForUser(id, clock.instant());
        notifications.queuePasswordChanged(user);
    }

}
