package com.rit.performance.exception;

import org.springframework.http.HttpStatus;

/** Commits the failed-attempt counter without changing the password. */
public class InvalidPasswordOtpException extends ApplicationException {
    public InvalidPasswordOtpException(boolean exhausted) {
        super(HttpStatus.BAD_REQUEST, exhausted ? "OTP_ATTEMPTS_EXCEEDED" : "INVALID_OTP",
                exhausted ? "Too many incorrect codes. Please request a new setup or reset link."
                        : "The verification code is incorrect.");
    }
}
