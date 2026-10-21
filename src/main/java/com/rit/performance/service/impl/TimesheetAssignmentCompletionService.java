package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TimesheetAssignmentCompletionService {
    private final TimesheetRepository timesheets;
    private final TimesheetEmployeeProjectDayRepository days;

    @Transactional
    public void cancelAfter(TimesheetEmployeeProject setup, LocalDate endDate, Long updatedBy) {
        if (timesheets.existsByEmployee_IdAndTimesheetEmployeeProjectIsNullAndWeekEndDateAfter(
                setup.getEmployee().getId(), endDate))
            throw new InvalidOperationException(
                    "Resolve legacy weekly timesheets without a setup ID before completing this assignment");
        var remainingDays = days.findByTimesheetEmployeeProjectIdOrderByWorkDate(setup.getId()).stream()
                .filter(day -> day.getWorkDate().isAfter(endDate))
                .filter(day -> day.getStatus() != TimesheetScheduleStatus.CANCELLED).toList();
        var remainingWeeks = timesheets.findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(setup.getId(), endDate)
                .stream().filter(week -> week.getStatus() != TimesheetStatus.CANCELLED).toList();
        if (remainingDays.stream().anyMatch(TimesheetEmployeeProjectDay::isLocked))
            throw new InvalidOperationException("Resolve locked schedule days after assignmentEndDate before completing");
        for (var week : remainingWeeks) {
            boolean wholeWeek = week.getWeekStartDate().isAfter(endDate);
            boolean protectedWeek = week.getSubmittedAt() != null || !week.getApprovals().isEmpty()
                    || week.getStatus() == TimesheetStatus.SUBMITTED
                    || week.getStatus() == TimesheetStatus.LEVEL1_APPROVED
                    || week.getStatus() == TimesheetStatus.APPROVED;
            boolean laterEntries = week.getEntries().stream()
                    .anyMatch(entry -> entry.getWorkDate().isAfter(endDate));
            if (protectedWeek || laterEntries || (wholeWeek && (
                    nonzero(week.getTotalHours()) || nonzero(week.getRegularHours())
                    || nonzero(week.getHolidayHours()) || nonzero(week.getLeaveHours()))))
                throw new InvalidOperationException(
                        "Resolve recorded hours or submitted/approved timesheet " + week.getId()
                                + " after assignmentEndDate before completing");
        }
        for (var day : remainingDays) {
            day.setStatus(TimesheetScheduleStatus.CANCELLED);
            day.setActive(false);
            day.setUpdatedBy(updatedBy);
        }
        for (var week : remainingWeeks) {
            if (week.getWeekStartDate().isAfter(endDate)) {
                week.setStatus(TimesheetStatus.CANCELLED);
                week.setUpdatedBy(updatedBy);
            }
        }
        days.saveAll(remainingDays);
        timesheets.saveAll(remainingWeeks);
    }

    private boolean nonzero(BigDecimal hours) {
        return hours != null && hours.signum() != 0;
    }
}
