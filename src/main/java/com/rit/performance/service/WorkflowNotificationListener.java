package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(propagation = Propagation.MANDATORY)
public class WorkflowNotificationListener {
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final AppUserDetailsService userDetails;
    @Value("${app.notifications.onboarding-reviewer-user-ids:}")
    private List<Long> onboardingReviewerIds = List.of();

    @EventListener
    public void leave(NotificationEvents.LeaveChanged event) {
        var r = event.request();
        String status = r.getStatus().name();
        String key = "LEAVE:" + r.getId() + ":" + r.getSubmittedAt() + ":" + status;
        Employee recipient = switch (r.getStatus()) {
            case SUBMITTED -> r.getLevel1Approver();
            case LEVEL1_APPROVED -> r.getLevel2Approver();
            case CANCELLED -> r.getLevel1Approver();
            default -> r.getEmployee();
        };
        boolean action = r.getStatus() == LeaveRequestStatus.SUBMITTED || r.getStatus() == LeaveRequestStatus.LEVEL1_APPROVED;
        employee(recipient, "LEAVE", "LEAVE_" + status, action ? "Leave approval needed" : "Leave " + status.toLowerCase(Locale.ROOT),
                name(r.getEmployee()) + "'s leave request (" + r.getFromDate() + " to " + r.getToDate() + ") "
                        + (action ? "is awaiting your approval. Open the request to review it." : "was " + status.toLowerCase(Locale.ROOT) + ". Open the request for details."),
                "LEAVE_REQUEST", r.getId(), key);
    }

    @EventListener
    public void timesheet(NotificationEvents.TimesheetChanged event) {
        var sheet = event.timesheet();
        String status = sheet.getStatus().name();
        String key = "TIMESHEET:" + sheet.getId() + ":" + sheet.getSubmittedAt() + ":" + status;
        Integer level = sheet.getStatus() == TimesheetStatus.SUBMITTED ? 1
                : sheet.getStatus() == TimesheetStatus.LEVEL1_APPROVED ? 2 : null;
        String message = name(sheet.getEmployee()) + "'s timesheet for " + sheet.getWeekStartDate();
        if (level != null) {
            sheet.getApprovals().stream().filter(a -> level.equals(a.getApprovalLevel()))
                    .map(TimesheetApproval::getApproverEmployee).forEach(recipient -> employee(recipient,
                            "TIMESHEET", "TIMESHEET_" + status, "Timesheet approval needed",
                            message + " is awaiting your approval. Open the timesheet to review it.", "TIMESHEET", sheet.getId(), key));
        } else {
            employee(sheet.getEmployee(), "TIMESHEET", "TIMESHEET_" + status,
                    "Timesheet " + status.toLowerCase(Locale.ROOT),
                    message + " was " + status.toLowerCase(Locale.ROOT) + ". Open the timesheet for details.",
                    "TIMESHEET", sheet.getId(), key);
        }
    }

    @EventListener
    public void onboarding(NotificationEvents.OnboardingChanged event) {
        var onboarding = event.onboarding();
        String key = "ONBOARDING:" + onboarding.getId() + ":" + onboarding.getVersion();
        if ("CHANGES_REQUESTED".equals(onboarding.getStatus())) {
            employee(onboarding.getEmployee(), "ONBOARDING", "ONBOARDING_CHANGES_REQUESTED",
                    "Onboarding corrections requested", "Review the requested corrections to your profile, update it and submit it again.",
                    "ONBOARDING", onboarding.getEmployee().getId(), key);
        } else if ("SUBMITTED".equals(onboarding.getStatus())) {
            var recipients = onboardingReviewerIds.isEmpty() ? users.findAllByOrderByIdAsc() : users.findAllById(onboardingReviewerIds);
            var eligible = recipients.stream().filter(this::active)
                    .filter(u -> "FULL".equalsIgnoreCase(u.getPortalAccess()))
                    .filter(u -> "ROLE_HR".equals(userDetails.roleAuthority(u.getRole()))
                            || (!onboardingReviewerIds.isEmpty() && "ROLE_ADMIN".equals(userDetails.roleAuthority(u.getRole()))))
                    .sorted(Comparator.comparing(User::getId))
                    .toList();
            if (eligible.isEmpty()) log.warn("No active onboarding notification reviewers for onboarding {}", onboarding.getId());
            eligible.forEach(u -> create(u, "ONBOARDING", "ONBOARDING_SUBMITTED", "Onboarding review needed",
                    name(onboarding.getEmployee()) + " submitted their profile. Open onboarding to review it.",
                    "ONBOARDING", onboarding.getEmployee().getId(), key));
        }
    }

    @EventListener
    public void review(NotificationEvents.ReviewAlert event) {
        employee(event.recipient(), "PERFORMANCE_REVIEW", event.eventType(), event.title(), event.message(),
                "EMPLOYEE_REVIEW", event.reviewId(), event.eventType() + ":" + event.occurrence());
    }

    private void employee(Employee employee, String category, String event, String title, String message,
                          String recordType, Long recordId, String key) {
        if (employee == null) return;
        users.findByEmployeeId(employee.getId()).filter(this::active).ifPresentOrElse(
                u -> create(u, category, event, title, message, recordType, recordId, key),
                () -> log.info("Skipping in-app event {} for employee {}: no active user account", event, employee.getId()));
    }

    private void create(User user, String category, String event, String title, String message,
                        String recordType, Long recordId, String key) {
        // Serializing by recipient makes the existence check safe across concurrent workflow transactions.
        var recipient = users.findForSecurityUpdate(user.getId()).filter(this::active).orElse(null);
        if (recipient == null || notifications.findForEvent(user.getId(), key).isPresent()) return;
        var n = new UserNotification();
        n.setRecipient(recipient); n.setCategory(category); n.setEventType(event);
        n.setTitle(title); n.setMessage(message); n.setRelatedRecordType(recordType); n.setRelatedRecordId(recordId);
        n.setDeduplicationKey(key); n.setCreatedOn(Instant.now());
        notifications.save(n);
    }

    private boolean active(User user) { return "ACTIVE".equalsIgnoreCase(user.getStatus()); }
    private String name(Employee employee) {
        return (Objects.toString(employee.getFirstName(), "") + " " + Objects.toString(employee.getLastName(), "")).trim();
    }
}
