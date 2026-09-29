package com.rit.performance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String retryAfter;

    public ApplicationException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApplicationException(HttpStatus status, String code, String message, String retryAfter) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }
}
