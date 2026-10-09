package com.rit.performance.service;

import com.rit.performance.dto.EmailNotificationRequest;
import com.rit.performance.dto.EmailNotificationResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailNotificationService {
    private final EmailNotificationRepository repository;
    private final ApplicationEmailFactory emailFactory;
    private final org.springframework.context.ApplicationEventPublisher events;
    public void queueOnboardingSubmitted(EmployeeOnboarding onboarding, boolean resubmission) {
        var email = emailFactory.onboardingSubmitted(onboarding, null, resubmission);
        // Empty primary recipient denotes a category-only notification.
        var notification = EmailNotification.builder().eventType(EmailEventType.ONBOARDING_SUBMITTED)
                .recipientEmail("").subject(email.subject()).body(email.body())
                .deduplicationKey("ONBOARDING_SUBMITTED:" + onboarding.getId() + ":"
                        + onboarding.getVersion()).build();
        if (!repository.existsByDeduplicationKey(notification.getDeduplicationKey())) repository.save(notification);
    }
    public void queuePasswordChanged(User user) {
        Employee employee = user.getEmployee();
        if (employee == null) return;
        ApplicationEmail email = emailFactory.passwordChanged(user);
        queue(EmailNotification.builder().eventType(EmailEventType.PASSWORD_CHANGED)
                .recipientEmail(employee.getEmail()).recipientName(employeeName(employee))
                .subject(email.subject()).body(email.body())
                .deduplicationKey("PASSWORD_CHANGED:" + user.getId() + ":" + UUID.randomUUID())
                .build());
    }
    public void queueCyclePublished(PerformanceCycles cycle, Employee employee, EmployeeReview review) {
        ApplicationEmail email = emailFactory.cyclePublished(cycle, employee);
        queue(EmailNotification.builder().eventType(EmailEventType.CYCLE_PUBLISHED)
                .recipientEmail(employee.getEmail()).recipientName(employeeName(employee))
                .subject(email.subject()).body(email.body())
                .employeeReviewId(review.getId()).cycleId(cycle.getId())
                .deduplicationKey("CYCLE_PUBLISHED:" + cycle.getId() + ":" + employee.getId()).build());
    }

    public void queueAssessmentReady(EmployeeReview review, EmployeeReviewAssessment assessment) {
        Employee reviewer = assessment.getAssessorEmployee();
        if (reviewer == null) return;
        ApplicationEmail email = emailFactory.assessmentReady(review, assessment);
        queue(EmailNotification.builder().eventType(EmailEventType.ASSESSMENT_READY)
                .recipientEmail(reviewer.getEmail()).recipientName(employeeName(reviewer))
                .subject(email.subject()).body(email.body())
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("ASSESSMENT_READY:" + assessment.getId()).build());
    }

    public void queueAssessmentReopened(EmployeeReview review, EmployeeReviewAssessment assessment,
            LocalDate newDueDate, String reason) {
        Employee reviewer = assessment.getAssessorEmployee();
        if (reviewer == null) return;
        ApplicationEmail email = emailFactory.assessmentReopened(review, assessment, newDueDate, reason);
        queue(EmailNotification.builder().eventType(EmailEventType.ASSESSMENT_REOPENED)
                .recipientEmail(reviewer.getEmail()).recipientName(employeeName(reviewer))
                .subject(email.subject()).body(email.body())
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("ASSESSMENT_REOPENED:" + assessment.getId() + ":" + UUID.randomUUID())
                .build());
    }

    public void queueResultPublished(FinalRating rating) {
        EmployeeReview review = rating.getEmployeeReview();
        Employee employee = review.getEmployee();
        ApplicationEmail email = emailFactory.resultPublished(rating);
        queue(EmailNotification.builder().eventType(EmailEventType.RESULT_PUBLISHED)
                .recipientEmail(employee.getEmail()).recipientName(employeeName(employee))
                .subject(email.subject()).body(email.body())
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("RESULT_PUBLISHED:" + rating.getId()).build());
    }

    public EmailNotificationResponse queueManual(EmailNotificationRequest request) {
        String recipientName = trim(request.getRecipientName());
        ApplicationEmail email = emailFactory.manualNotification(
                request.getRecipientEmail().trim(), recipientName, request.getSubject().trim(),
                request.getBody().trim(), trim(request.getActionUrl()));
        EmailNotification notification = EmailNotification.builder()
                .eventType(request.getEventType() == null ? EmailEventType.MANUAL : request.getEventType())
                .recipientEmail(email.recipient()).recipientName(recipientName)
                .subject(email.subject()).body(email.body())
                .employeeReviewId(request.getEmployeeReviewId())
                .cycleId(request.getCycleId()).deduplicationKey("MANUAL:" + UUID.randomUUID()).build();
        EmailNotification saved = repository.save(notification);
        if (saved.getEventType() == EmailEventType.MANUAL || saved.getEventType() == EmailEventType.REMINDER) {
            events.publishEvent(new NotificationEvents.BusinessEmailAlert(
                    email.withBell("GENERAL", saved.getEventType().name(), "NOTIFICATION", saved.getId())));
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EmailNotificationResponse> search(EmailEventType eventType, EmailDeliveryStatus status,
            String recipient, Long cycleId, Long reviewId, String query, Pageable pageable) {
        return repository.search(eventType, status, blankToNull(recipient), cycleId, reviewId,
                blankToNull(query), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmailNotificationResponse get(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email notification not found: " + id)));
    }

    public EmailNotificationResponse retry(Long id) {
        EmailNotification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email notification not found: " + id));
        notification.setStatus(EmailDeliveryStatus.PENDING);
        notification.setErrorMessage(null);
        notification.setNextAttemptDate(null);
        return toResponse(repository.save(notification));
    }

    private void queue(EmailNotification notification) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) return;
        if (!repository.existsByDeduplicationKey(notification.getDeduplicationKey())) repository.save(notification);
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public EmailNotificationResponse toResponse(EmailNotification email) {
        return EmailNotificationResponse.builder().id(email.getId()).eventType(email.getEventType())
                .recipientEmail(email.getRecipientEmail()).recipientName(email.getRecipientName())
                .subject(email.getSubject()).body(email.getBody()).footer(email.getFooter())
                .actionUrl(email.getActionUrl()).employeeReviewId(email.getEmployeeReviewId())
                .cycleId(email.getCycleId()).status(email.getStatus()).retryCount(email.getRetryCount())
                .errorMessage(email.getErrorMessage()).createdDate(email.getCreatedOn())
                .sentDate(email.getSentDate()).nextAttemptDate(email.getNextAttemptDate()).build();
    }
}
