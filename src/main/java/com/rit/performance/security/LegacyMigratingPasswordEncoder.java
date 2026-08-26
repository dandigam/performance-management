package com.rit.performance.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Accepts legacy plaintext passwords once, then signals that they must be upgraded to BCrypt.
 */
public class LegacyMigratingPasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (isBcrypt(encodedPassword)) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return MessageDigest.isEqual(
                rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                encodedPassword.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !isBcrypt(encodedPassword) || bcrypt.upgradeEncoding(encodedPassword);
    }

    private boolean isBcrypt(String value) {
        return value != null && (value.startsWith("$2a$")
                || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
