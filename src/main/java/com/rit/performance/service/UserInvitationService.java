package com.rit.performance.service;

import com.rit.performance.entity.PasswordResetToken;
import com.rit.performance.entity.User;
import com.rit.performance.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserInvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PasswordResetTokenRepository tokens;
    private final ApplicationEventPublisher events;
    private final ApplicationEmailFactory emailFactory;
    private final Clock clock;

    @Value("${app.mail.base-url:http://localhost:5173}")
    private String frontendUrl;

    public void send(User user, String recipient) {
        tokens.invalidateAllForUser(user.getId());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(PasswordResetRateLimiter.hash(raw));
        token.setExpiresAt(clock.instant().plus(Duration.ofHours(24)));
        tokens.save(token);

        String invitationUrl = frontendUrl.replaceAll("/$", "")
                + "/reset-password?token=" + raw;
        events.publishEvent(emailFactory.userInvitation(recipient, user.getUsername(), invitationUrl));
    }
}
