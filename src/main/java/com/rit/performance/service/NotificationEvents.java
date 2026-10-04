package com.rit.performance.service;

import com.rit.performance.entity.*;

/** Synchronous domain events; entities are consumed within the originating transaction. */
public final class NotificationEvents {
    private NotificationEvents() {}
    public record LeaveChanged(LeaveRequest request) {}
    public record TimesheetChanged(Timesheet timesheet) {}
    public record OnboardingChanged(EmployeeOnboarding onboarding) {}
    public record ReviewAlert(Employee recipient, Long reviewId, String eventType, String title,
                              String message, String occurrence) {}
}
