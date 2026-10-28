package com.rit.performance.service;

import java.util.List;

/** Prepared template emails; no managed entities cross into the async worker. */
public record PerformancePublicationEmails(List<ApplicationEmail> emails) {
    public PerformancePublicationEmails {
        emails = List.copyOf(emails);
    }
}
