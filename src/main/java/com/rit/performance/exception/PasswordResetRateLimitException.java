package com.rit.performance.exception;

public class PasswordResetRateLimitException extends RuntimeException {
    public PasswordResetRateLimitException() {
        super("Too many password reset requests. Please try again later.");
    }
}
