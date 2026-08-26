package com.rit.performance.service;

import com.rit.performance.entity.RefreshToken;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public IssuedRefreshToken issue(User user) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        repository.save(RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build());
        return new IssuedRefreshToken(rawToken, user, expiresAt);
    }

    public IssuedRefreshToken rotate(String rawToken) {
        RefreshToken existing = findUsable(rawToken);
        existing.setRevokedAt(Instant.now());
        repository.save(existing);
        return issue(existing.getUser());
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private RefreshToken findUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new AuthenticationException("Refresh token is expired or revoked");
        }
        if (!"ACTIVE".equalsIgnoreCase(token.getUser().getStatus())) {
            throw new AuthenticationException("User account is inactive");
        }
        return token;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(String value, User user, Instant expiresAt) {
    }
}
