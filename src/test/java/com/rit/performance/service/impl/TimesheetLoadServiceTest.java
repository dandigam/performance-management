package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.TimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetLoadServiceTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final TimesheetRepository timesheets = mock(TimesheetRepository.class);
    private final LocalDate current = LocalDate.of(2026, 9, 6);
    private final TimesheetGenerationServiceImpl service = new TimesheetGenerationServiceImpl(timesheets,
            Clock.fixed(Instant.parse("2026-09-12T12:00:00Z"), ZoneOffset.UTC), mock(com.rit.performance.repository.TimesheetEmployeeProjectRepository.class), employees, mock(com.rit.performance.repository.SowMilestonePositionAssignmentRepository.class));

    @BeforeEach
    void employeeExists() {
        var employee = new Employee();
        employee.setId(3L);
        when(employees.findById(3L)).thenReturn(Optional.of(employee));
    }

    @Test
    void statusTabsUseCurrentWeekCutoffAndCorrectStatuses() {
        for (String status : List.of("NEW", "draft", " DRAFT ")) {
            service.getAll(3L, status);
        }
        verify(timesheets, times(3)).findForStatusTab(3L, current, List.of(TimesheetStatus.DRAFT));
        service.getAll(3L, "PENDING");
        verify(timesheets).findForStatusTab(3L, current,
                List.of(TimesheetStatus.SUBMITTED, TimesheetStatus.LEVEL1_APPROVED));
        service.getAll(3L, "REJECTED");
        verify(timesheets).findForStatusTab(3L, current, List.of(TimesheetStatus.REJECTED));
        service.getAll(3L, "ALL");
        verify(timesheets).findForStatusTab(3L, current, java.util.Arrays.stream(TimesheetStatus.values()).filter(s -> s != TimesheetStatus.CANCELLED).toList());
        service.getAll(null, "ALL");
        verify(timesheets).findForStatusTab(null, current, java.util.Arrays.stream(TimesheetStatus.values()).filter(s -> s != TimesheetStatus.CANCELLED).toList());
    }

    @Test
    void invalidStatusDoesNotQueryTimesheets() {
        assertThatThrownBy(() -> service.getAll(3L, "UNKNOWN"))
                .isInstanceOf(com.rit.performance.exception.InvalidOperationException.class);
        verifyNoInteractions(timesheets);
    }

    @Test
    void futureWeekDetailIsRejectedEvenWithTimesheetId() {
        assertThatThrownBy(() -> service.getWeek(3L, current.plusWeeks(1), 1L))
                .isInstanceOf(com.rit.performance.exception.InvalidOperationException.class)
                .hasMessageContaining("Future");
        verifyNoInteractions(timesheets);
    }

    @Test
    void detailWithoutIdFindsEmployeeWeek() {
        var saved = sheet(current, TimesheetStatus.DRAFT);
        var employee = new Employee();
        employee.setId(3L);
        saved.setEmployee(employee);
        when(timesheets.findAllByEmployeeIdAndWeekStartDate(3L, current)).thenReturn(List.of(saved));
        assertThat(service.getWeek(3L, current, null).getTimesheetId()).isEqualTo(saved.getId());
        verify(timesheets, never()).findOneById(any());
    }

    @Test
    void cleanupTargetsOnlyDistinctDeletedWeeksForEmployee() {
        service.cleanupEmptyDraftWeeks(3L, List.of(current.plusDays(1), current.plusDays(2),
                current.plusWeeks(1)));
        verify(timesheets).deleteEmptyDraftWeek(3L, current);
        verify(timesheets).deleteEmptyDraftWeek(3L, current.plusWeeks(1));
        verifyNoMoreInteractions(timesheets);
    }

    @Test
    void cleanupWithoutExplicitDeletionsDoesNothing() {
        service.cleanupEmptyDraftWeeks(3L, List.of());
        verifyNoInteractions(timesheets);
    }

    @Test
    void cleanupRejectsUnknownEmployee() {
        assertThatThrownBy(() -> service.cleanupEmptyDraftWeeks(999L, List.of(current)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(timesheets);
    }

    @Test
    void createsOneHeaderPerDistinctWeekForMonthAndReusesOverlappingWeeks() {
        var dates = LocalDate.of(2026, 9, 1).datesUntil(LocalDate.of(2026, 10, 1)).toList();
        service.ensureWeeklyTimesheets(3L, dates);
        for (var start = LocalDate.of(2026, 8, 30); start.isBefore(LocalDate.of(2026, 10, 1)); start = start.plusWeeks(1))
            verify(timesheets).insertWeeklyHeaderIfMissing(3L, start, start.plusDays(6));
        verifyNoMoreInteractions(timesheets);
        clearInvocations(timesheets);
        service.ensureWeeklyTimesheets(3L, LocalDate.of(2026, 9, 20)
                .datesUntil(LocalDate.of(2026, 10, 1)).toList());
        verify(timesheets).insertWeeklyHeaderIfMissing(3L, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 26));
        verify(timesheets).insertWeeklyHeaderIfMissing(3L, LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 3));
        verifyNoMoreInteractions(timesheets);
    }

    @Test
    void emptyScheduleChangesDoNotCreateHeaders() {
        service.ensureWeeklyTimesheets(3L, List.of());
        verifyNoInteractions(timesheets);
    }

    private Timesheet sheet(LocalDate start, TimesheetStatus status) {
        var sheet = new Timesheet();
        sheet.setId(start.toEpochDay());
        sheet.setWeekStartDate(start);
        sheet.setWeekEndDate(start.plusDays(6));
        sheet.setStatus(status);
        return sheet;
    }

}
