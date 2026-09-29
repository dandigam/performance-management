package com.rit.performance.service;

import com.rit.performance.dto.*;
import com.rit.performance.entity.EmailNotification;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingRequestChangesService {
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService summaries;
    private final ApplicationEmailFactory emails;
    private final EmailNotificationRepository notifications;

    @Transactional
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public OnboardingResponse requestChanges(Long employeeId, OnboardingRequestChangesRequest request, String reviewer) {
        var onboarding = onboardings.findForUpdateByEmployeeId(employeeId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for this employee."));
        if (request.version() == null || request.version() != onboarding.getVersion())
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_VERSION_CONFLICT",
                    "The onboarding record has changed. Refresh the review and try again.");
        if (!"SUBMITTED".equals(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_SUBMITTED",
                    "Changes can only be requested for submitted onboarding.");
        onboarding.setReviewComments(request.comments().trim());
        onboarding.setReviewedBy(reviewer);
        onboarding.setReviewedAt(java.time.Instant.now());
        onboarding.setStatus("CHANGES_REQUESTED");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding); // JPA increments @Version while holding the row lock.
        var email = emails.onboardingChangesRequested(onboarding.getEmployee(), onboarding.getReviewComments());
        notifications.save(EmailNotification.builder().eventType(EmailEventType.ONBOARDING_CHANGES_REQUESTED)
                .recipientEmail(email.recipient()).subject(email.subject()).body(email.body())
                .deduplicationKey("ONBOARDING_CHANGES_REQUESTED:" + onboarding.getId() + ":" + onboarding.getVersion())
                .build());
        return summaries.responseFor(onboarding.getEmployee(), onboarding);
    }
}
