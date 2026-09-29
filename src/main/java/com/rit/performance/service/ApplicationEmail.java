package com.rit.performance.service;

/** Transient email data delivered after a successful transaction. */
public record ApplicationEmail(String recipient, String subject, String body, boolean html, String category) {
    public ApplicationEmail(String recipient, String subject, String body, boolean html) {
        this(recipient, subject, body, html, null);
    }
    public ApplicationEmail withCategory(String category) {
        return new ApplicationEmail(recipient, subject, body, html, category);
    }
    public ApplicationEmail(String recipient, String subject, String body) {
        this(recipient, subject, body, false);
    }
}
