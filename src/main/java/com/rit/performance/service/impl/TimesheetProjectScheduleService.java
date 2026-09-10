package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetDailyOverrideRequest;
import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetDailyOverrideResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.HolidayRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectDayRepository;
import com.rit.performance.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class TimesheetProjectScheduleService {
    private static final BigDecimal MAX_HOURS = new BigDecimal("24.00");
    private final TimesheetEmployeeProjectDayRepository dayRepository;
    private final HolidayRepository holidayRepository;
    private final TimesheetRepository timesheetRepository;

    void regenerate(TimesheetEmployeeProject project, TimesheetEmployeeProjectRequest request) {
        Map<LocalDate, TimesheetDailyOverrideRequest> overrides = overrides(request, project);
        boolean sparseSchedule = request.getDefaultHoursPerDay() == null;
        Map<LocalDate, TimesheetEmployeeProjectDay> current = dayRepository
                .findByTimesheetEmployeeProjectIdOrderByWorkDate(project.getId()).stream()
                .collect(Collectors.toMap(TimesheetEmployeeProjectDay::getWorkDate, Function.identity()));
        Map<LocalDate, Holiday> holidays = holidays(project);

        for (TimesheetEmployeeProjectDay day : current.values()) {
            boolean shouldDeactivate = day.getWorkDate().isBefore(project.getStartDate())
                    || day.getWorkDate().isAfter(project.getEndDate())
                    || (sparseSchedule && !overrides.containsKey(day.getWorkDate()));
            if (!shouldDeactivate) continue;
            if (day.isLocked() || timesheetRepository.isDateLocked(
                    project.getEmployee().getId(), day.getWorkDate())) {
                day.setLocked(true);
            } else {
                day.setActive(false);
            }
        }

        for (LocalDate date = project.getStartDate(); !date.isAfter(project.getEndDate()); date = date.plusDays(1)) {
            if (sparseSchedule && !overrides.containsKey(date)) continue;
            TimesheetEmployeeProjectDay day = current.get(date);
            if (day != null && (day.isLocked() || timesheetRepository.isDateLocked(
                    project.getEmployee().getId(), date))) {
                day.setLocked(true);
                dayRepository.save(day);
                continue;
            }
            if (day == null) day = new TimesheetEmployeeProjectDay();
            apply(day, project, date, overrides.get(date), holidays.get(date));
            BigDecimal other = dayRepository.sumOtherProjectHours(project.getEmployee().getId(), date, project.getId());
            if (other.add(day.getScheduledHours()).compareTo(MAX_HOURS) > 0)
                throw new InvalidOperationException("Total scheduled hours exceed 24 for employee "
                        + project.getEmployee().getId() + " on " + date);
            dayRepository.save(day);
        }
        dayRepository.saveAll(current.values());
        dayRepository.flush();
    }

    List<TimesheetDailyOverrideResponse> overrides(TimesheetEmployeeProject project) {
        return dayRepository.findByTimesheetEmployeeProjectIdOrderByWorkDate(project.getId()).stream()
                .filter(TimesheetEmployeeProjectDay::isActive)
                .filter(day -> isOverride(day, project.getDefaultHoursPerDay()))
                .map(day -> TimesheetDailyOverrideResponse.builder()
                        .workDate(day.getWorkDate())
                        .scheduledHours(day.getScheduledHours())
                        .dayType(day.getDayType())
                        .build())
                .toList();
    }

    private boolean isOverride(TimesheetEmployeeProjectDay day, BigDecimal defaultHours) {
        if (defaultHours == null) return true;
        if (day.getHoliday() != null && day.getDayType() == TimesheetDayType.HOLIDAY
                && day.getScheduledHours().signum() == 0) return false;
        if (weekend(day.getWorkDate()) && day.getDayType() == TimesheetDayType.NON_WORKING_DAY
                && day.getScheduledHours().signum() == 0) return false;
        TimesheetDayType defaultType = defaultHours.signum() == 0
                ? TimesheetDayType.NON_WORKING_DAY : TimesheetDayType.WORKING_DAY;
        return day.getScheduledHours().compareTo(defaultHours) != 0 || day.getDayType() != defaultType;
    }

    private Map<LocalDate, TimesheetDailyOverrideRequest> overrides(
            TimesheetEmployeeProjectRequest request, TimesheetEmployeeProject project) {
        Map<LocalDate, TimesheetDailyOverrideRequest> result = new HashMap<>();
        for (TimesheetDailyOverrideRequest override : Optional.ofNullable(
                request.getDailyOverrides()).orElseGet(List::of)) {
            if (override.getWorkDate().isBefore(project.getStartDate())
                    || override.getWorkDate().isAfter(project.getEndDate()))
                throw new InvalidOperationException("Daily override is outside effective range: " + override.getWorkDate());
            if (result.put(override.getWorkDate(), override) != null)
                throw new DuplicateResourceException("Duplicate daily override: " + override.getWorkDate());
        }
        return result;
    }

    private void apply(TimesheetEmployeeProjectDay day, TimesheetEmployeeProject project,
                       LocalDate date, TimesheetDailyOverrideRequest override, Holiday holiday) {
        day.setTimesheetEmployeeProject(project);
        day.setEmployee(project.getEmployee());
        day.setSow(project.getSow());
        day.setMilestone(project.getMilestone());
        day.setWorkDate(date);
        day.setActive(true);
        if (override != null) {
            day.setScheduledHours(override.getScheduledHours());
            day.setDayType(override.getDayType() == null ? dayType(date, holiday) : override.getDayType());
            day.setHoliday(day.getDayType() == TimesheetDayType.HOLIDAY ? holiday : null);
        } else if (holiday != null) {
            set(day, BigDecimal.ZERO, TimesheetDayType.HOLIDAY, holiday);
        } else if (weekend(date)) {
            set(day, BigDecimal.ZERO, TimesheetDayType.NON_WORKING_DAY, null);
        } else {
            TimesheetDayType type = project.getDefaultHoursPerDay().signum() == 0
                    ? TimesheetDayType.NON_WORKING_DAY : TimesheetDayType.WORKING_DAY;
            set(day, project.getDefaultHoursPerDay(), type, null);
        }
    }

    private void set(TimesheetEmployeeProjectDay day, BigDecimal hours, TimesheetDayType type,
                     Holiday holiday) {
        day.setScheduledHours(hours); day.setDayType(type); day.setHoliday(holiday);
    }

    private Map<LocalDate, Holiday> holidays(TimesheetEmployeeProject project) {
        String mode = project.getEmployee().getWorkMode();
        List<Holiday> list = mode == null || mode.isBlank()
                ? holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(project.getStartDate(), project.getEndDate())
                : holidayRepository.findByLocationTypeIgnoreCaseAndHolidayDateBetweenOrderByHolidayDateAsc(
                        mode, project.getStartDate(), project.getEndDate());
        return list.stream().filter(Holiday::isActive).collect(Collectors.toMap(
                Holiday::getHolidayDate, Function.identity(), (first, ignored) -> first));
    }

    private TimesheetDayType dayType(LocalDate date, Holiday holiday) {
        if (holiday != null) return TimesheetDayType.HOLIDAY;
        return weekend(date) ? TimesheetDayType.NON_WORKING_DAY : TimesheetDayType.WORKING_DAY;
    }

    private boolean weekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
