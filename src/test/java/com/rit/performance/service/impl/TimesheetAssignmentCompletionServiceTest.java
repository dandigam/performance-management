package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetAssignmentCompletionServiceTest {
    final TimesheetRepository weeks = mock(TimesheetRepository.class);
    final TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
    final TimesheetAssignmentCompletionService service = new TimesheetAssignmentCompletionService(weeks, days);
    final LocalDate end = LocalDate.of(2026, 1, 28);
    final TimesheetEmployeeProject setup = TimesheetEmployeeProject.builder().id(10L)
            .employee(employee()).build();

    private Employee employee() {
        var employee = new Employee();
        employee.setId(2L);
        return employee;
    }

    Timesheet sheet(LocalDate start) {
        return Timesheet.builder().id(3L).timesheetEmployeeProject(setup)
                .weekStartDate(start).weekEndDate(start.plusDays(6)).build();
    }

    @Test void cancelsOnlyLaterDaysAndFullWeeksAndKeepsBoundary() {
        var lastDay = TimesheetEmployeeProjectDay.builder().workDate(end).build();
        var laterDay = TimesheetEmployeeProjectDay.builder().workDate(end.plusDays(1)).build();
        var partial = sheet(LocalDate.of(2026, 1, 25));
        var future = sheet(LocalDate.of(2026, 2, 1));
        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(10L)).thenReturn(List.of(lastDay, laterDay));
        when(weeks.findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(10L, end)).thenReturn(List.of(partial, future));
        service.cancelAfter(setup, end, 7L);
        assertThat(lastDay.getStatus()).isEqualTo(TimesheetScheduleStatus.ACTIVE);
        assertThat(laterDay.getStatus()).isEqualTo(TimesheetScheduleStatus.CANCELLED);
        assertThat(laterDay.isActive()).isFalse();
        assertThat(partial.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(future.getStatus()).isEqualTo(TimesheetStatus.CANCELLED);
        verify(weeks).findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(10L, end);
    }

    @Test void rejectsLaterEntriesBeforeChangingAnything() {
        var day = TimesheetEmployeeProjectDay.builder().workDate(end.plusDays(1)).build();
        var partial = sheet(LocalDate.of(2026, 1, 25));
        partial.addEntry(TimesheetEntry.builder().workDate(end.plusDays(1)).hours(BigDecimal.ONE).build());
        when(days.findByTimesheetEmployeeProjectIdOrderByWorkDate(10L)).thenReturn(List.of(day));
        when(weeks.findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(10L, end)).thenReturn(List.of(partial));
        assertThatThrownBy(() -> service.cancelAfter(setup, end, 7L)).isInstanceOf(InvalidOperationException.class);
        assertThat(day.isActive()).isTrue();
        verify(days, never()).saveAll(any());
    }

    @Test void rejectsSubmittedFutureWeek() {
        var future = sheet(LocalDate.of(2026, 2, 1));
        future.setStatus(TimesheetStatus.SUBMITTED);
        when(weeks.findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(10L, end)).thenReturn(List.of(future));
        assertThatThrownBy(() -> service.cancelAfter(setup, end, 7L)).isInstanceOf(InvalidOperationException.class);
        assertThat(future.getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
    }

    @Test void rejectsUnlinkedLegacyWeeks() {
        when(weeks.existsByEmployee_IdAndTimesheetEmployeeProjectIsNullAndWeekEndDateAfter(2L, end)).thenReturn(true);
        assertThatThrownBy(() -> service.cancelAfter(setup, end, 7L))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("legacy");
        verifyNoInteractions(days);
    }

    @Test void keepsWorkedEntriesOnLastDay() {
        var partial = sheet(LocalDate.of(2026, 1, 25));
        partial.addEntry(TimesheetEntry.builder().workDate(end).hours(BigDecimal.ONE).build());
        when(weeks.findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(10L, end)).thenReturn(List.of(partial));
        service.cancelAfter(setup, end, 7L);
        assertThat(partial.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(partial.getEntries()).hasSize(1);
    }
}
