package com.rit.performance.service;

/** Transient email data delivered after a successful transaction. */
public record ApplicationEmail(String recipient, String subject, String body, boolean html, String category,
                               BellAlert bellAlert) {
    public ApplicationEmail(String recipient, String subject, String body, boolean html, String category) {
        this(recipient, subject, body, html, category, null);
    }
    public record BellAlert(String category, String eventType, String recordType, Long recordId,
                            java.util.List<Long> employeeIds, boolean includeAdmins, String occurrence) {}
    public ApplicationEmail withBell(String category, String eventType, String recordType, Long recordId) {
        return withBell(category, eventType, recordType, recordId, java.util.List.of(), false);
    }
    public ApplicationEmail withBell(String category, String eventType, String recordType, Long recordId,
                                     java.util.List<Long> employeeIds, boolean includeAdmins) {
        return new ApplicationEmail(recipient, subject, body, html, this.category,
                new BellAlert(category, eventType, recordType, recordId, java.util.List.copyOf(employeeIds),
                        includeAdmins, java.util.UUID.randomUUID().toString()));
    }
    public ApplicationEmail(String recipient, String subject, String body, boolean html) {
        this(recipient, subject, body, html, null);
    }
    public ApplicationEmail withCategory(String category) {
        return new ApplicationEmail(recipient, subject, body, html, category, bellAlert);
    }
    public ApplicationEmail withBellAlert(BellAlert alert) {
        return new ApplicationEmail(recipient, subject, body, html, category, alert);
    }
    public ApplicationEmail(String recipient, String subject, String body) {
        this(recipient, subject, body, false);
    }
}
