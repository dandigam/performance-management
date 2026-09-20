package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetEntriesRequest;
import com.rit.performance.dto.response.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TimesheetEntryService {
    private final TimesheetRepository timesheets;
    private final HolidayRepository holidays;
    private final LeaveRequestRepository leaveRequests;
    private final Clock clock;

    private record Key(LocalDate date, TimesheetEntryType type) {}

    @Transactional
    public TimesheetEntriesResponse save(Long id, TimesheetEntriesRequest request) {
        if (request == null || request.entries() == null || request.entries().isEmpty())
            throw new InvalidOperationException("entries must not be empty");
        if (request.status() != null && request.status() != TimesheetStatus.DRAFT
                && request.status() != TimesheetStatus.SUBMITTED)
            throw new InvalidOperationException("status must be DRAFT or SUBMITTED");
        var sheet = timesheets.findForEntryUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found: " + id));
        if (sheet.getStatus() != TimesheetStatus.DRAFT || sheet.getSubmittedAt() != null
                || !sheet.getApprovals().isEmpty())
            throw new InvalidOperationException("Only unsubmitted DRAFT timesheets can be edited");
        var setup = sheet.getTimesheetEmployeeProject();
        if (setup == null)
            throw new InvalidOperationException("Link the timesheet to a setup before saving entries");
        if (request.status() == TimesheetStatus.SUBMITTED
                && (setup.getLevel1Approver() == null || setup.getLevel2Approver() == null))
            throw new InvalidOperationException("Configure both approval levels before submitting the timesheet");
        if (sheet.getWeekStartDate().isAfter(LocalDate.now(clock)))
            throw new InvalidOperationException("Future timesheet weeks cannot be edited");
        Map<Key, TimesheetEntry> existing = new HashMap<>();
        for (var entry : sheet.getEntries()) {
            if (existing.put(new Key(entry.getWorkDate(), entry.getEntryType()), entry) != null)
                throw new InvalidOperationException("Resolve duplicate existing entries for the same date and type");
        }
        Set<Key> seen = new HashSet<>();
        Map<Key, Holiday> resolvedHolidays = new HashMap<>();
        Map<Key, BigDecimal> proposed = new HashMap<>();
        existing.forEach((key, entry) -> proposed.put(key, entry.getHours()));
        for (var input : request.entries()) {
            if (input == null || input.workDate() == null || input.entryType() == null || input.hours() == null)
                throw new InvalidOperationException("Each entry requires workDate, entryType and hours");
            var key = new Key(input.workDate(), input.entryType());
            if (!seen.add(key)) throw new InvalidOperationException("Duplicate workDate and entryType");
            if (input.hours().signum() < 0 || input.hours().compareTo(BigDecimal.valueOf(24)) > 0
                    || input.hours().stripTrailingZeros().scale() > 2)
                throw new InvalidOperationException("hours must be between 0 and 24 with at most two decimal places");
            var date = input.workDate();
            if (date.isBefore(sheet.getWeekStartDate()) || date.isAfter(sheet.getWeekEndDate())
                    || date.isBefore(setup.getEffectiveStartDate()) || date.isAfter(setup.getEffectiveEndDate()))
                throw new InvalidOperationException("workDate must be within the timesheet week and assignment dates");
            var day = setup.getDailySchedules().stream().filter(d -> date.equals(d.getWorkDate()))
                    .findFirst().orElseThrow(() -> new InvalidOperationException("workDate must have an active schedule"));
            if (!day.isActive() || day.getStatus() == TimesheetScheduleStatus.CANCELLED || day.isLocked())
                throw new InvalidOperationException("Cancelled, inactive or locked schedule days cannot be edited");
            if (input.leaveId() != null) {
                if (input.entryType() != TimesheetEntryType.LEAVE)
                    throw new InvalidOperationException("leaveId is allowed only for LEAVE entries");
                LeaveRequest leave = leaveRequests.findByIdAndEmployeeId(input.leaveId(), sheet.getEmployee().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + input.leaveId()));
                if (leave.getStatus() != LeaveRequestStatus.APPROVED)
                    throw new InvalidOperationException("Leave request must be finally approved before it can be used in a timesheet");
                LeaveRequestDay leaveDay = leave.getDays().stream()
                        .filter(item -> date.equals(item.getLeaveDate())).findFirst()
                        .orElseThrow(() -> new InvalidOperationException("Leave request does not include work date " + date));
                if (input.hours().signum() <= 0 || input.hours().compareTo(leaveDay.getRequestedHours()) > 0)
                    throw new InvalidOperationException("Timesheet leave hours must be positive and cannot exceed approved hours for " + date);
            }
            if (input.entryType() == TimesheetEntryType.HOLIDAY) {
                if (input.holidayId() == null) throw new InvalidOperationException("holidayId is required for HOLIDAY");
                var holiday = holidays.findById(input.holidayId())
                        .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + input.holidayId()));
                if (!holiday.isActive() || !date.equals(holiday.getHolidayDate()))
                    throw new InvalidOperationException("Holiday must be active and match workDate");
                resolvedHolidays.put(key, holiday);
            } else if (input.holidayId() != null) {
                throw new InvalidOperationException("holidayId is allowed only for HOLIDAY entries");
            }
            proposed.put(key, input.hours());
        }
        Map<LocalDate, BigDecimal> dailyTotals = new HashMap<>();
        proposed.forEach((key, hours) -> dailyTotals.merge(key.date(), hours, BigDecimal::add));
        if (dailyTotals.values().stream().anyMatch(hours -> hours.compareTo(BigDecimal.valueOf(24)) > 0))
            throw new InvalidOperationException("Combined daily hours cannot exceed 24");
        for (var input : request.entries()) {
            var key = new Key(input.workDate(), input.entryType());
            var entry = existing.get(key);
            if (entry == null) {
                entry = new TimesheetEntry();
                entry.setWorkDate(input.workDate());
                entry.setEntryType(input.entryType());
                sheet.addEntry(entry);
            }
            entry.setHours(input.hours());
            entry.setHoliday(resolvedHolidays.get(key));
            entry.setLeaveId(input.leaveId());
            entry.setSow(setup.getSow());
        }
        sheet.recalculateHours();
        if (request.status() == TimesheetStatus.SUBMITTED) {
            sheet.addApproval(TimesheetApproval.builder()
                    .approvalLevel(1).approverEmployee(setup.getLevel1Approver())
                    .status(TimesheetApprovalStatus.PENDING).build());
            sheet.addApproval(TimesheetApproval.builder()
                    .approvalLevel(2).approverEmployee(setup.getLevel2Approver())
                    .status(TimesheetApprovalStatus.PENDING).build());
            sheet.setStatus(TimesheetStatus.SUBMITTED);
            sheet.setSubmittedAt(LocalDateTime.now(clock));
        }
        timesheets.saveAndFlush(sheet);
        var entries = sheet.getEntries().stream()
                .sorted(Comparator.comparing(TimesheetEntry::getWorkDate).thenComparing(TimesheetEntry::getEntryType))
                .map(entry -> TimesheetWeekEntryResponse.builder().entryId(entry.getId())
                        .workDate(entry.getWorkDate()).entryType(entry.getEntryType()).hours(entry.getHours())
                        .sowId(entry.getSow() == null ? null : entry.getSow().getId())
                        .holidayId(entry.getHoliday() == null ? null : entry.getHoliday().getId())
                        .leaveId(entry.getLeaveId()).build()).toList();
        return new TimesheetEntriesResponse(sheet.getId(), sheet.getStatus(), sheet.getRegularHours(),
                sheet.getHolidayHours(), sheet.getLeaveHours(), sheet.getTotalHours(), entries);
    }
}
