package com.rit.performance.exception;

public class AccountUnavailableException extends RuntimeException {
    public AccountUnavailableException(String message) {
        super(message);
    }
}
