package com.rit.performance.exception;

public class InvalidOperationException extends RuntimeException {
    private final String code;

    public InvalidOperationException(String message) {
        this("INVALID_OPERATION", message);
    }

    public InvalidOperationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
