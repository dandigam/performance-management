package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetScheduleDateRequest;
import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetDailyOverrideResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.TimesheetEmployeeProjectDayRepository;
import com.rit.performance.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class TimesheetProjectScheduleService {
    private static final BigDecimal MAX_HOURS = new BigDecimal("24.00");
    private final TimesheetEmployeeProjectDayRepository dayRepository;
    private final TimesheetRepository timesheetRepository;

    // Participates in TimesheetEmployeeProjectServiceImpl's transaction for the entire batch.
    void applyChanges(TimesheetEmployeeProject project, TimesheetEmployeeProjectRequest request) {
        Map<LocalDate, TimesheetScheduleDateRequest> scheduled = new LinkedHashMap<>();
        for (TimesheetScheduleDateRequest date : request.getScheduleDates()) {
            if (date.getWorkDate().isBefore(project.getEffectiveStartDate())
                    || date.getWorkDate().isAfter(project.getEffectiveEndDate()))
                throw new InvalidOperationException("Schedule date is outside effective range: " + date.getWorkDate());
            if (scheduled.put(date.getWorkDate(), date) != null)
                throw new DuplicateResourceException("Duplicate schedule date: " + date.getWorkDate());
        }
        Set<LocalDate> deleted = new HashSet<>();
        for (var date : request.getDeletedDates()) {
            if (!deleted.add(date.getWorkDate()))
                throw new DuplicateResourceException("Duplicate deleted date: " + date.getWorkDate());
            if (scheduled.containsKey(date.getWorkDate()))
                throw new InvalidOperationException("Date cannot appear in both scheduleDates and deletedDates: "
                        + date.getWorkDate());
        }
        Set<LocalDate> changedDates = new HashSet<>(scheduled.keySet());
        changedDates.addAll(deleted);
        if (changedDates.isEmpty()) return;

        Map<LocalDate, TimesheetEmployeeProjectDay> current = dayRepository
                .findByTimesheetEmployeeProjectIdAndWorkDateIn(project.getId(), changedDates).stream()
                .collect(Collectors.toMap(TimesheetEmployeeProjectDay::getWorkDate, Function.identity()));

        // Only explicitly deleted dates are removed, including dates outside the new range.
        for (LocalDate date : deleted) {
            TimesheetEmployeeProjectDay day = current.get(date);
            if ((day != null && day.isLocked())
                    || timesheetRepository.isDateLocked(project.getEmployee().getId(), date, project.getId()))
                throw new InvalidOperationException("Cannot delete a locked schedule date: " + date);
            if (day != null) dayRepository.delete(day);
        }

        for (var entry : scheduled.entrySet()) {
            LocalDate date = entry.getKey();
            TimesheetEmployeeProjectDay day = current.get(date);
            if ((day != null && day.isLocked())
                    || timesheetRepository.isDateLocked(project.getEmployee().getId(), date, project.getId()))
                throw new InvalidOperationException("Cannot change a locked schedule date: " + date);
            TimesheetScheduleDateRequest schedule = entry.getValue();
            BigDecimal other = dayRepository.sumOtherProjectHours(project.getEmployee().getId(), date, project.getId());
            if (other.add(schedule.getScheduledHours()).compareTo(MAX_HOURS) > 0)
                throw new InvalidOperationException("Total scheduled hours exceed 24 for employee "
                        + project.getEmployee().getId() + " on " + date);
            if (day == null) day = new TimesheetEmployeeProjectDay();
            day.setTimesheetEmployeeProject(project);
            day.setEmployee(project.getEmployee());
            day.setSow(project.getSow());
            day.setMilestone(project.getMilestone());
            day.setWorkDate(date);
            day.setScheduledHours(schedule.getScheduledHours());
            day.setDayType(schedule.getScheduledHours().signum() > 0
                    ? TimesheetDayType.WORKING_DAY : TimesheetDayType.NON_WORKING_DAY);
            day.setHoliday(null);
            day.setActive(true);
            dayRepository.save(day);
        }
        dayRepository.flush();
    }

    List<TimesheetDailyOverrideResponse> overrides(TimesheetEmployeeProject project) {
        return dayRepository.findByTimesheetEmployeeProjectIdOrderByWorkDate(project.getId()).stream()
                .filter(TimesheetEmployeeProjectDay::isActive)
                .map(day -> TimesheetDailyOverrideResponse.builder()
                        .workDate(day.getWorkDate())
                        .scheduledHours(day.getScheduledHours())
                        .dayType(day.getDayType())
                        .build())
                .toList();
    }
}
