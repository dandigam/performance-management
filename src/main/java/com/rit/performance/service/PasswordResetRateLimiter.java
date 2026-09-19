package com.rit.performance.service;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/** Bounded per-instance limiter. Keys are hashed; never use reset tokens as keys. */
@Component
public class PasswordResetRateLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    private record Window(Instant expiresAt, int count) {}
    public PasswordResetRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized boolean allow(String key, int limit, Duration duration) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
        String digest = hash(key);
        Window current = windows.get(digest);
        if (current == null) {
            if (windows.size() >= 10000) return false;
            windows.put(digest, new Window(now.plus(duration), 1));
            return true;
        }
        if (current.count() >= limit) return false;
        windows.put(digest, new Window(current.expiresAt(), current.count() + 1));
        return true;
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
    }
}
