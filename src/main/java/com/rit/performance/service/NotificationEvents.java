package com.rit.performance.service;

import com.rit.performance.entity.*;

/** Domain events. Review alerts snapshot recipient IDs for background processing. */
public final class NotificationEvents {
    private NotificationEvents() {}
    public record LeaveChanged(LeaveRequest request) {}
    public record TimesheetChanged(Timesheet timesheet) {}
    public record OnboardingChanged(EmployeeOnboarding onboarding) {}
    public record BusinessEmailAlert(ApplicationEmail email) {}
    public record ReviewAlert(Long recipientEmployeeId, Long reviewId, String eventType, String title,
                              String message, String occurrence) {
        public ReviewAlert(Employee recipient, Long reviewId, String eventType, String title,
                           String message, String occurrence) {
            this(recipient == null ? null : recipient.getId(), reviewId, eventType, title, message, occurrence);
        }
    }
}
