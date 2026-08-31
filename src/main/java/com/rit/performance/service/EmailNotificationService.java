package com.rit.performance.service;

import com.rit.performance.dto.EmailNotificationRequest;
import com.rit.performance.dto.EmailNotificationResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.mail.footer:Regards, RIT Performance Management}")
    private String defaultFooter;
    @Value("${app.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    public void queueCyclePublished(PerformanceCycles cycle, Employee employee, EmployeeReview review) {
        queue(EmailNotification.builder().eventType(EmailEventType.CYCLE_PUBLISHED)
                .recipientEmail(employee.getEmail()).recipientName(employeeName(employee))
                .subject(cycle.getCycleName() + " is now open")
                .body(greeting(employee) + "\n\nThe performance review cycle \"" + cycle.getCycleName()
                        + "\" is now open. Please sign in and complete your self-review.")
                .footer(defaultFooter).actionUrl(url("/login"))
                .employeeReviewId(review.getId()).cycleId(cycle.getId())
                .deduplicationKey("CYCLE_PUBLISHED:" + cycle.getId() + ":" + employee.getId()).build());
    }

    public void queueAssessmentReady(EmployeeReview review, EmployeeReviewAssessment assessment) {
        Employee reviewer = assessment.getAssessorEmployee();
        if (reviewer == null) return;
        String roleName = assessment.getAssessorRole() == null ? "reviewer" : assessment.getAssessorRole().getName();
        String deadlineDetails = assessment.getDueDate() == null ? ""
                : "\n\nDue date: " + assessment.getDueDate()
                    + (assessment.getReopenReason() == null || assessment.getReopenReason().isBlank()
                        ? "" : "\nExtension reason: " + assessment.getReopenReason());
        queue(EmailNotification.builder().eventType(EmailEventType.ASSESSMENT_READY)
                .recipientEmail(reviewer.getEmail()).recipientName(employeeName(reviewer))
                .subject(employeeName(review.getEmployee()) + "'s review is ready")
                .body(greeting(reviewer) + "\n\n" + employeeName(review.getEmployee())
                        + "'s performance review is ready for your " + roleName + " assessment."
                        + deadlineDetails)
                .footer(defaultFooter).actionUrl(url("/login"))
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("ASSESSMENT_READY:" + assessment.getId()).build());
    }

    public void queueAssessmentReopened(EmployeeReview review, EmployeeReviewAssessment assessment,
            LocalDate newDueDate, String reason) {
        Employee reviewer = assessment.getAssessorEmployee();
        if (reviewer == null) return;
        String roleName = assessment.getAssessorRole() == null
                ? "review" : assessment.getAssessorRole().getName() + " assessment";
        queue(EmailNotification.builder().eventType(EmailEventType.ASSESSMENT_REOPENED)
                .recipientEmail(reviewer.getEmail()).recipientName(employeeName(reviewer))
                .subject("Assessment reopened until " + newDueDate)
                .body(greeting(reviewer) + "\n\nThe " + roleName + " for "
                        + employeeName(review.getEmployee()) + " in \""
                        + review.getPerformanceCycle().getCycleName() + "\" has been reopened."
                        + "\n\nNew due date: " + newDueDate
                        + "\nReason: " + reason)
                .footer(defaultFooter).actionUrl(url("/login"))
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("ASSESSMENT_REOPENED:" + assessment.getId() + ":" + UUID.randomUUID())
                .build());
    }

    public void queueResultPublished(FinalRating rating) {
        EmployeeReview review = rating.getEmployeeReview();
        Employee employee = review.getEmployee();
        queue(EmailNotification.builder().eventType(EmailEventType.RESULT_PUBLISHED)
                .recipientEmail(employee.getEmail()).recipientName(employeeName(employee))
                .subject("Your performance review result is available")
                .body(greeting(employee) + "\n\nYour result for \""
                        + review.getPerformanceCycle().getCycleName() + "\" has been published."
                        + " Sign in to view your result.")
                .footer(defaultFooter).actionUrl(url("/login"))
                .employeeReviewId(review.getId()).cycleId(review.getPerformanceCycle().getId())
                .deduplicationKey("RESULT_PUBLISHED:" + rating.getId()).build());
    }

    public EmailNotificationResponse queueManual(EmailNotificationRequest request) {
        EmailNotification notification = EmailNotification.builder()
                .eventType(request.getEventType() == null ? EmailEventType.MANUAL : request.getEventType())
                .recipientEmail(request.getRecipientEmail().trim()).recipientName(trim(request.getRecipientName()))
                .subject(request.getSubject().trim()).body(request.getBody().trim())
                .footer(request.getFooter() == null || request.getFooter().isBlank()
                        ? defaultFooter : request.getFooter().trim())
                .actionUrl(trim(request.getActionUrl())).employeeReviewId(request.getEmployeeReviewId())
                .cycleId(request.getCycleId()).deduplicationKey("MANUAL:" + UUID.randomUUID()).build();
        return toResponse(repository.save(notification));
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

    private String greeting(Employee employee) {
        return "Hello " + employeeName(employee) + ",";
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private String url(String path) {
        return baseUrl.replaceAll("/$", "") + path;
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
