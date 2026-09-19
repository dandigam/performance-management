package com.rit.performance.service;

/** Transient delivery data: do not log or persist this event. */
public final class PasswordResetEmail {
    private final String email;
    private final String link;
    public PasswordResetEmail(String email, String link) { this.email = email; this.link = link; }
    public String email() { return email; }
    public String link() { return link; }
}
