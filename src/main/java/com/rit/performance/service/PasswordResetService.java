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
    public void sendOtp(String raw, String remoteAddress) {
        if (!limiter.allow("otp-ip:" + remoteAddress, 20, Duration.ofHours(1))) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many verification code requests. Please try again later.", "3600");
        }
        PasswordResetToken token = lockValidToken(raw);
        if (token.getOtpAttempts() >= 5) throw new com.rit.performance.exception.InvalidPasswordOtpException(true);
        if (token.getOtpSendCount() >= 3) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "OTP_SEND_LIMIT_EXCEEDED",
                    "The code send limit has been reached. Please request a new setup or reset link.");
        }
        var now = clock.instant();
        if (token.getOtpSentAt() != null && token.getOtpSentAt().plusSeconds(60).isAfter(now)) {
            long wait = Math.max(1, Duration.between(now, token.getOtpSentAt().plusSeconds(60)).toSeconds() + 1);
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON",
                    "Please wait before requesting another code.", Long.toString(wait));
        }
        User user = token.getUser();
        String recipient = user.getEmployee() == null ? user.getUsername() : user.getEmployee().getEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_FOUND", "No account email is available.");
        }
        String otp = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        token.setOtpHash(encoder.encode(otp));
        var expires = now.plus(Duration.ofMinutes(10));
        token.setOtpExpiresAt(expires.isBefore(token.getExpiresAt()) ? expires : token.getExpiresAt());
        token.setOtpSentAt(now);
        token.setOtpSendCount(token.getOtpSendCount() + 1);
        // Resending never restores the failed-attempt budget.
        tokens.save(token);
        events.publishEvent(emailFactory.passwordOtp(recipient.trim(), otp, "INVITED".equalsIgnoreCase(user.getStatus())));
    }

    /** Older callers cannot bypass OTP verification. */
    public void reset(String raw, String password) {
        throw new ApplicationException(HttpStatus.BAD_REQUEST, "OTP_REQUIRED",
                "Request a verification code and include it with your new password.");
    }

    @Transactional(noRollbackFor = com.rit.performance.exception.InvalidPasswordOtpException.class)
    public void reset(String raw, String password, String otp) {
        PasswordResetToken token = lockValidToken(raw);
        User user = token.getUser();
        Long id = user.getId();
        if (token.getOtpAttempts() >= 5) throw new com.rit.performance.exception.InvalidPasswordOtpException(true);
        if (token.getOtpHash() == null || otp == null || otp.isBlank()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "OTP_REQUIRED",
                    "Request a verification code and include it with your new password.");
        }
        if (token.getOtpExpiresAt() == null || !token.getOtpExpiresAt().isAfter(clock.instant())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "The code has expired. Please request another code.");
        }
        if (!otp.matches("[0-9]{6}") || !encoder.matches(otp, token.getOtpHash())) {
            token.setOtpAttempts(token.getOtpAttempts() + 1);
            tokens.save(token);
            // This specific exception commits only the attempt counter. No password changes have occurred.
            throw new com.rit.performance.exception.InvalidPasswordOtpException(token.getOtpAttempts() >= 5);
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


    private PasswordResetToken lockValidToken(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw invalidLink();
        String hash = PasswordResetRateLimiter.hash(raw);
        Long id = tokens.findUserIdByHash(hash).orElseThrow(this::invalidLink);
        // Preserve the user-then-token lock order used by login, reset and refresh.
        User user = users.findForSecurityUpdate(id).orElseThrow(this::invalidLink);
        PasswordResetToken token = tokens.findByTokenHash(hash).orElseThrow(this::invalidLink);
        if (token.isUsed() || !token.getExpiresAt().isAfter(clock.instant())
                || !("ACTIVE".equalsIgnoreCase(user.getStatus()) || "INVITED".equalsIgnoreCase(user.getStatus()))) {
            throw invalidLink();
        }
        return token;
    }

    private ApplicationException invalidLink() {
        return new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_LINK", INVALID_LINK);
    }
}
