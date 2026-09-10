package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetDailyOverrideRequest;
import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.HolidayRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectDayRepository;
import com.rit.performance.repository.TimesheetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimesheetProjectScheduleServiceTest {

    @Test
    void generatesDefaultsWeekendsAndOverridesWithoutChangingLockedRows() {
        TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
        HolidayRepository holidays = mock(HolidayRepository.class);
        TimesheetRepository timesheets = mock(TimesheetRepository.class);
        TimesheetProjectScheduleService service = new TimesheetProjectScheduleService(days, holidays, timesheets);
        TimesheetEmployeeProject project = project(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 6));

        TimesheetEmployeeProjectDay locked = new TimesheetEmployeeProjectDay();
        locked.setTimesheetEmployeeProject(project);
        locked.setEmployee(project.getEmployee());
        locked.setSow(project.getSow());
        locked.setMilestone(project.getMilestone());
        locked.setWorkDate(LocalDate.of(2026, 9, 6));
        locked.setScheduledHours(new BigDecimal("7.00"));
        locked.setDayType(TimesheetDayType.WORKING_DAY);
        locked.setLocked(true);
        locked.setActive(true);

        TimesheetDailyOverrideRequest override = new TimesheetDailyOverrideRequest();
        override.setWorkDate(LocalDate.of(2026, 9, 4));
        override.setScheduledHours(new BigDecimal("4.00"));
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setDefaultHoursPerDay(new BigDecimal("8.00"));
        request.setDailyOverrides(List.of(override));

        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(100L)).thenReturn(List.of(locked));
        when(days.sumOtherProjectHours(anyLong(), any(), anyLong())).thenReturn(BigDecimal.ZERO);
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());

        service.regenerate(project, request);

        ArgumentCaptor<TimesheetEmployeeProjectDay> saved = ArgumentCaptor.forClass(TimesheetEmployeeProjectDay.class);
        verify(days, atLeast(3)).save(saved.capture());
        TimesheetEmployeeProjectDay friday = saved.getAllValues().stream()
                .filter(day -> LocalDate.of(2026, 9, 4).equals(day.getWorkDate())).findFirst().orElseThrow();
        TimesheetEmployeeProjectDay saturday = saved.getAllValues().stream()
                .filter(day -> LocalDate.of(2026, 9, 5).equals(day.getWorkDate())).findFirst().orElseThrow();
        assertThat(friday.getScheduledHours()).isEqualByComparingTo("4.00");
        assertThat(friday.getDayType()).isEqualTo(TimesheetDayType.WORKING_DAY);
        assertThat(saturday.getScheduledHours()).isZero();
        assertThat(saturday.getDayType()).isEqualTo(TimesheetDayType.NON_WORKING_DAY);
        assertThat(locked.getScheduledHours()).isEqualByComparingTo("7.00");
    }

    @Test
    void rejectsCombinedProjectHoursAboveTwentyFour() {
        TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
        HolidayRepository holidays = mock(HolidayRepository.class);
        TimesheetRepository timesheets = mock(TimesheetRepository.class);
        TimesheetProjectScheduleService service = new TimesheetProjectScheduleService(days, holidays, timesheets);
        TimesheetEmployeeProject project = project(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 4));
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setDefaultHoursPerDay(new BigDecimal("8.00"));

        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(100L)).thenReturn(List.of());
        when(days.sumOtherProjectHours(anyLong(), any(), anyLong())).thenReturn(new BigDecimal("20.00"));
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.regenerate(project, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceed 24");
    }

    @Test
    void zeroDefaultMakesUnselectedWeekdaysNonWorking() {
        TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
        HolidayRepository holidays = mock(HolidayRepository.class);
        TimesheetRepository timesheets = mock(TimesheetRepository.class);
        TimesheetProjectScheduleService service = new TimesheetProjectScheduleService(days, holidays, timesheets);
        TimesheetEmployeeProject project = project(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 27));
        project.setDefaultHoursPerDay(BigDecimal.ZERO);
        TimesheetDailyOverrideRequest selected = new TimesheetDailyOverrideRequest();
        selected.setWorkDate(LocalDate.of(2026, 8, 26));
        selected.setScheduledHours(new BigDecimal("2.00"));
        selected.setDayType(TimesheetDayType.WORKING_DAY);
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setDefaultHoursPerDay(BigDecimal.ZERO);
        request.setDailyOverrides(List.of(selected));

        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(100L)).thenReturn(List.of());
        when(days.sumOtherProjectHours(anyLong(), any(), anyLong())).thenReturn(BigDecimal.ZERO);
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());

        service.regenerate(project, request);

        ArgumentCaptor<TimesheetEmployeeProjectDay> saved = ArgumentCaptor.forClass(TimesheetEmployeeProjectDay.class);
        verify(days, times(2)).save(saved.capture());
        TimesheetEmployeeProjectDay unselected = saved.getAllValues().stream()
                .filter(day -> LocalDate.of(2026, 8, 27).equals(day.getWorkDate())).findFirst().orElseThrow();
        assertThat(unselected.getScheduledHours()).isZero();
        assertThat(unselected.getDayType()).isEqualTo(TimesheetDayType.NON_WORKING_DAY);
    }

    @Test
    void nullDefaultGeneratesOnlySelectedOverrideDates() {
        TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
        HolidayRepository holidays = mock(HolidayRepository.class);
        TimesheetRepository timesheets = mock(TimesheetRepository.class);
        TimesheetProjectScheduleService service = new TimesheetProjectScheduleService(days, holidays, timesheets);
        TimesheetEmployeeProject project = project(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 28));
        project.setDefaultHoursPerDay(BigDecimal.ZERO);

        TimesheetDailyOverrideRequest selected = new TimesheetDailyOverrideRequest();
        selected.setWorkDate(LocalDate.of(2026, 8, 26));
        selected.setScheduledHours(new BigDecimal("2.00"));
        selected.setDayType(TimesheetDayType.WORKING_DAY);
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setDailyOverrides(List.of(selected));

        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(100L)).thenReturn(List.of());
        when(days.sumOtherProjectHours(anyLong(), any(), anyLong())).thenReturn(BigDecimal.ZERO);
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());

        service.regenerate(project, request);

        ArgumentCaptor<TimesheetEmployeeProjectDay> saved = ArgumentCaptor.forClass(TimesheetEmployeeProjectDay.class);
        verify(days, times(1)).save(saved.capture());
        assertThat(saved.getValue().getWorkDate()).isEqualTo(LocalDate.of(2026, 8, 26));
        assertThat(saved.getValue().getScheduledHours()).isEqualByComparingTo("2.00");
        assertThat(saved.getValue().getDayType()).isEqualTo(TimesheetDayType.WORKING_DAY);
    }

    private TimesheetEmployeeProject project(LocalDate start, LocalDate end) {
        Employee employee = new Employee(); employee.setId(10L);
        Sow sow = new Sow(); sow.setId(20L);
        SowMilestone milestone = new SowMilestone(); milestone.setId(30L);
        TimesheetEmployeeProject project = new TimesheetEmployeeProject();
        project.setId(100L); project.setEmployee(employee); project.setSow(sow); project.setMilestone(milestone);
        project.setStartDate(start); project.setEndDate(end); project.setDefaultHoursPerDay(new BigDecimal("8.00"));
        return project;
    }
}
