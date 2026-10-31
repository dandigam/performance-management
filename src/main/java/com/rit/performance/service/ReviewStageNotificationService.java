package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.PerformanceCycleTimeline;
import com.rit.performance.entity.User;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeReviewAssessmentRepository;
import com.rit.performance.repository.PerformanceCycleTimelineRepository;
import com.rit.performance.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class ReviewStageNotificationService {
    private final EmployeeReviewAssessmentRepository assessments;
    private final PerformanceCycleTimelineRepository timelines;
    private final UserRepository users;
    private final EmailNotificationService notifications;
    private final EntityManager entityManager;
    private final Clock clock;
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;
    private static final String COOLDOWN = "Please wait 60 seconds before resending this notification.";

    public record NotificationInfo(String reviewerName, String email, boolean canResend, String unavailableReason) {}

    public NotificationInfo get(Long reviewId, Long stageId) {
        return info(find(reviewId, stageId, false));
    }

    @Transactional
    public void resend(Long reviewId, Long stageId, String expectedEmail) {
        EmployeeReviewAssessment assessment = find(reviewId, stageId, true);
        // Keep the confirmation address stable until the queue entry commits.
        entityManager.refresh(assessment.getAssessorEmployee(), LockModeType.PESSIMISTIC_READ);
        NotificationInfo info = info(assessment);
        if (!normalize(expectedEmail).equals(normalize(info.email()))) {
            throw new ApplicationException(HttpStatus.CONFLICT, "REVIEWER_EMAIL_CHANGED",
                    "The reviewer's email changed. Refresh and confirm the current address.");
        }
        if (COOLDOWN.equals(info.unavailableReason())) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_TOO_SOON", COOLDOWN, "60");
        }
        if (!info.canResend()) {
            throw new ApplicationException(HttpStatus.CONFLICT, "NOTIFICATION_UNAVAILABLE", info.unavailableReason());
        }
        notifications.resendAssessmentReady(assessment);
    }

    private EmployeeReviewAssessment find(Long reviewId, Long stageId, boolean lock) {
        EmployeeReviewAssessment assessment = lock ? entityManager.find(EmployeeReviewAssessment.class, stageId, LockModeType.PESSIMISTIC_WRITE)
                : assessments.findById(stageId).orElse(null);
        if (assessment == null || !Objects.equals(reviewId, assessment.getEmployeeReview().getId())) {
            throw new ResourceNotFoundException("Review stage not found for this review");
        }
        return assessment;
    }

    private NotificationInfo info(EmployeeReviewAssessment assessment) {
        Employee reviewer = assessment.getAssessorEmployee();
        String name = (Objects.toString(reviewer.getFirstName(), "") + " "
                + Objects.toString(reviewer.getLastName(), "")).trim();
        String reason = unavailableReason(assessment);
        return new NotificationInfo(name, reviewer.getEmail(), reason == null, reason);
    }

    private String unavailableReason(EmployeeReviewAssessment assessment) {
        if (!mailEnabled) {
            return "Email delivery is disabled.";
        }
        String stage = AssessmentAssigneeResolver.stageCode(assessment.getAssessorRole());
        if (!stage.startsWith("REPORTING_MANAGER_")) {
            return "Only manager review notifications can be resent.";
        }
        if (assessment.getStatus() == EmployeeReviewStatus.SUBMITTED) {
            return "This assessment is already submitted.";
        }
        Employee reviewer = assessment.getAssessorEmployee();
        if (!"ACTIVE".equalsIgnoreCase(reviewer.getStatus())) {
            return "The assigned reviewer is inactive.";
        }
        User account = users.findByEmployeeId(reviewer.getId()).orElse(null);
        if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return "The reviewer needs an active login account; complete login setup first.";
        }
        String email = normalize(reviewer.getEmail());
        if (email.length() > 150 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return "The reviewer has no valid email address.";
        }
        EmployeeReview review = assessment.getEmployeeReview();
        if (review.getAssessments().stream().anyMatch(a -> a.getAssessmentLevel() < assessment.getAssessmentLevel()
                && a.getStatus() != EmployeeReviewStatus.SUBMITTED)) {
            return "A previous assessment must be submitted first.";
        }
        List<PerformanceCycleTimeline> phases = timelines.findByPerformanceCycleIdOrderByDisplayOrderAsc(review.getPerformanceCycle().getId());
        LocalDate due = EmployeeReviewServiceImpl.effectiveDueDate(assessment, phases);
        if (due != null && due.isBefore(LocalDate.now(clock))) {
            return "The assessment deadline has passed; extend it first.";
        }
        if (notifications.isAssessmentResendCoolingDown(assessment.getId(), clock)) {
            return COOLDOWN;
        }
        return null;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
