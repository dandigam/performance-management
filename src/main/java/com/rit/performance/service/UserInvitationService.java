package com.rit.performance.service;

import com.rit.performance.entity.PasswordResetToken;
import com.rit.performance.entity.User;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.PasswordResetTokenRepository;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PasswordResetTokenRepository tokens;
    private final ApplicationEventPublisher events;
    private final ApplicationEmailFactory emailFactory;
    private final Clock clock;
    private final UserRepository users;

    @Value("${app.mail.base-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public Optional<ApplicationEmail> ensurePerformanceLogin(Employee employee, PerformanceCycles cycle) {
        User account = users.findByEmployeeId(employee.getId()).orElseThrow(() ->
                new InvalidOperationException("Employee " + employee.getId()
                        + " has no login account. HR must create one before publication."));
        User lockedAccount = users.findForSecurityUpdate(account.getId()).orElseThrow(() ->
                new InvalidOperationException("Login account is no longer available for employee " + employee.getId()));
        if ("ACTIVE".equalsIgnoreCase(lockedAccount.getStatus())) {
            return Optional.empty();
        }
        if (!"INVITED".equalsIgnoreCase(lockedAccount.getStatus())) {
            throw new InvalidOperationException("Employee " + employee.getId()
                    + " has an unavailable login account (" + lockedAccount.getStatus()
                    + "). HR must resolve it before publication.");
        }
        String recipient = employee.getEmail() == null ? "" : employee.getEmail().trim();
        if (recipient.length() > 254 || !recipient.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new InvalidOperationException("Employee " + employee.getId()
                    + " needs a valid email address for login setup before publication.");
        }
        // This check runs under the same account lock used by explicit setup and password reset.
        if (!tokens.existsByUserIdAndUsedFalseAndExpiresAtAfter(lockedAccount.getId(), clock.instant())) {
            String invitationUrl = createSetupLink(lockedAccount);
            return Optional.of(emailFactory.cyclePublished(cycle, employee, invitationUrl));
        }
        return Optional.empty();
    }

    public void send(User user, String recipient) {
        String invitationUrl = createSetupLink(user);
        events.publishEvent(emailFactory.userInvitation(recipient, user.getUsername(), invitationUrl));
    }

    private String createSetupLink(User user) {
        tokens.invalidateAllForUser(user.getId());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(PasswordResetRateLimiter.hash(raw));
        token.setExpiresAt(clock.instant().plus(Duration.ofHours(24)));
        tokens.save(token);

        return frontendUrl.replaceAll("/$", "")
                + "/reset-password?token=" + raw;
    }
}
