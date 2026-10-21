package com.rit.performance.service;

import com.rit.performance.dto.request.ChangePasswordRequest;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService notifications;

    @Transactional
    public void change(String username, ChangePasswordRequest request) {
        if (username == null || username.isBlank())
            throw new AuthenticationException("Authentication is required.");
        var user = users.findForAuthentication(username)
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new AuthenticationException("User account is not available."));
        if (request == null || request.currentPassword() == null || request.currentPassword().isBlank())
            throw new InvalidOperationException("Current password is required.");
        // BCrypt rejects oversized inputs; treat these as a failed password check.
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.currentPassword(), user.getPassword()))
            throw new InvalidOperationException("Current password is incorrect.");
        String password = request.newPassword();
        PasswordPolicy.validate(password);
        if (passwordEncoder.matches(password, user.getPassword()))
            throw new InvalidOperationException("New password must be different from the current password.");
        user.setPassword(passwordEncoder.encode(password));
        users.save(user);
        notifications.queuePasswordChanged(user);
    }
}
