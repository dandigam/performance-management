package com.rit.performance.service;

import com.rit.performance.exception.InvalidOperationException;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String password) {
        if (password == null || password.isBlank())
            throw new InvalidOperationException("New password is required.");
        if (password.codePointCount(0, password.length()) < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidOperationException("New password must contain at least 8 characters and at most 72 UTF-8 bytes.");
    }
}
