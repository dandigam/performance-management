package com.rit.performance.service.impl;

import com.rit.performance.dto.request.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.TimesheetEmployeeProjectDayRepository;
import com.rit.performance.repository.TimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimesheetProjectScheduleServiceTest {
    private final TimesheetEmployeeProjectDayRepository days = mock(TimesheetEmployeeProjectDayRepository.class);
    private final TimesheetRepository timesheets = mock(TimesheetRepository.class);
    private final TimesheetProjectScheduleService service = new TimesheetProjectScheduleService(days, timesheets);
    private final Map<String, TimesheetEmployeeProjectDay> stored = new HashMap<>();
    private final TimesheetEmployeeProject project = project(100L);

    @BeforeEach
    void setupRepository() {
        when(days.findByTimesheetEmployeeProjectIdAndWorkDateIn(anyLong(), anyCollection()))
                .thenAnswer(call -> {
                    Long id = call.getArgument(0);
                    Collection<LocalDate> dates = call.getArgument(1);
                    return stored.values().stream().filter(day ->
                            day.getTimesheetEmployeeProject().getId().equals(id)
                                    && dates.contains(day.getWorkDate())).toList();
                });
        when(days.sumOtherProjectHours(anyLong(), any(), anyLong())).thenReturn(BigDecimal.ZERO);
        when(days.save(any())).thenAnswer(call -> {
            TimesheetEmployeeProjectDay day = call.getArgument(0);
            stored.put(key(day), day);
            return day;
        });
        doAnswer(call -> {
            stored.remove(key(call.getArgument(0)));
            return null;
        }).when(days).delete(any(TimesheetEmployeeProjectDay.class));
    }

    @Test
    void upsertsSelectedDatesDeletesOnlyExplicitDatesAndPreservesOtherAssignments() {
        TimesheetEmployeeProjectDay updated = seed(project, 1, "8");
        TimesheetEmployeeProjectDay unchanged = seed(project, 2, "6.5");
        seed(project, 3, "8");
        TimesheetEmployeeProjectDay other = seed(project(200L), 3, "4");
        TimesheetEmployeeProjectRequest request = request(schedule(1, "7.5"), schedule(4, "2"));
        request.setDeletedDates(List.of(deleted(3)));

        service.applyChanges(project, request);

        assertThat(stored).hasSize(4);
        assertThat(stored.get("100:2026-09-01")).isSameAs(updated);
        assertThat(updated.getScheduledHours()).isEqualByComparingTo("7.5");
        assertThat(stored.get("100:2026-09-02")).isSameAs(unchanged);
        assertThat(unchanged.getScheduledHours()).isEqualByComparingTo("6.5");
        assertThat(stored).doesNotContainKey("100:2026-09-03");
        assertThat(stored.get("200:2026-09-03")).isSameAs(other);
        assertThat(stored.get("100:2026-09-04").getScheduledHours()).isEqualByComparingTo("2");
    }

    @Test
    void deletingOneOf150DatesDoesNotRewriteTheRemaining149() {
        for (int offset = 0; offset < 150; offset++) {
            TimesheetEmployeeProjectDay day = new TimesheetEmployeeProjectDay();
            day.setTimesheetEmployeeProject(project);
            day.setScheduledHours(new BigDecimal("8"));
            day.setWorkDate(project.getStartDate().plusDays(offset));
            stored.put(key(day), day);
        }
        TimesheetEmployeeProjectRequest request = request();
        request.setDeletedDates(List.of(deleted(3)));

        service.applyChanges(project, request);

        assertThat(stored).hasSize(149).doesNotContainKey("100:2026-09-03");
        verify(days, never()).save(any());
        verify(days).findByTimesheetEmployeeProjectIdAndWorkDateIn(100L, Set.of(date(3)));
    }

    @Test
    void emptyListsAreNoOpEvenWhenRangeAndDefaultHoursChange() {
        TimesheetEmployeeProjectDay unchanged = seed(project, 20, "8");
        project.setEndDate(date(10));
        project.setDefaultHoursPerDay(new BigDecimal("4"));
        TimesheetEmployeeProjectRequest request = request();
        request.setDefaultHoursPerDay(new BigDecimal("4"));

        service.applyChanges(project, request);

        assertThat(stored).hasSize(1);
        assertThat(unchanged.getScheduledHours()).isEqualByComparingTo("8");
        verifyNoInteractions(timesheets);
        verify(days, never()).findByTimesheetEmployeeProjectIdAndWorkDateIn(anyLong(), anyCollection());
        verify(days, never()).save(any());
        verify(days, never()).delete(any(TimesheetEmployeeProjectDay.class));
    }

    @Test
    void explicitDeletionCanRemoveADateOutsideNewRangeAndIsIdempotent() {
        seed(project, 20, "8");
        project.setEndDate(date(10));
        TimesheetEmployeeProjectRequest request = request();
        request.setDeletedDates(List.of(deleted(20), deleted(21)));

        service.applyChanges(project, request);
        service.applyChanges(project, request);

        assertThat(stored).isEmpty();
        verify(days, times(1)).delete(any(TimesheetEmployeeProjectDay.class));
    }

    @Test
    void creationInsertsOnlyListedDatesWithoutGeneratingZeroHourRows() {
        TimesheetEmployeeProjectRequest request = request(schedule(1, "8"), schedule(5, "6.5"));
        request.setDefaultHoursPerDay(new BigDecimal("8"));

        service.applyChanges(project, request);

        assertThat(stored).hasSize(2);
        assertThat(stored.values()).allSatisfy(day -> {
            assertThat(day.isActive()).isTrue();
            assertThat(day.getDayType()).isEqualTo(TimesheetDayType.WORKING_DAY);
            assertThat(day.getEmployee()).isSameAs(project.getEmployee());
            assertThat(day.getSow()).isSameAs(project.getSow());
            assertThat(day.getMilestone()).isSameAs(project.getMilestone());
        });
    }

    @Test
    void rejectsDateInBothListsBeforeWriting() {
        TimesheetEmployeeProjectRequest request = request(schedule(1, "8"));
        request.setDeletedDates(List.of(deleted(1)));
        assertThatThrownBy(() -> service.applyChanges(project, request))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("both");
        verify(days, never()).save(any());
        verify(days, never()).delete(any(TimesheetEmployeeProjectDay.class));
    }

    @Test
    void rejectsDuplicateScheduleDates() {
        assertThatThrownBy(() -> service.applyChanges(project, request(schedule(1, "8"), schedule(1, "6"))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejectsDuplicateDeletedDates() {
        TimesheetEmployeeProjectRequest request = request();
        request.setDeletedDates(List.of(deleted(1), deleted(1)));
        assertThatThrownBy(() -> service.applyChanges(project, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejectsUpsertOutsideEffectiveRange() {
        project.setEndDate(date(10));
        assertThatThrownBy(() -> service.applyChanges(project, request(schedule(11, "8"))))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("outside effective range");
    }

    @Test
    void rejectsCombinedHoursOver24BeforeUpdatingStoredHours() {
        TimesheetEmployeeProjectDay existing = seed(project, 1, "2");
        when(days.sumOtherProjectHours(10L, date(1), 100L)).thenReturn(new BigDecimal("20"));
        assertThatThrownBy(() -> service.applyChanges(project, request(schedule(1, "8"))))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("exceed 24");
        assertThat(existing.getScheduledHours()).isEqualByComparingTo("2");
    }

    @Test
    void rejectsLockedUpsertInsteadOfSilentlyIgnoringIt() {
        seed(project, 1, "8").setLocked(true);
        assertThatThrownBy(() -> service.applyChanges(project, request(schedule(1, "6"))))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("locked");
        verify(days, never()).save(any());
    }

    @Test
    void doesNotInsertIntoASubmittedTimesheetDate() {
        when(timesheets.isDateLocked(10L, date(1))).thenReturn(true);
        assertThatThrownBy(() -> service.applyChanges(project, request(schedule(1, "6"))))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("locked");
    }

    private TimesheetEmployeeProjectDay seed(TimesheetEmployeeProject assignment, int number, String hours) {
        TimesheetEmployeeProjectDay day = new TimesheetEmployeeProjectDay();
        day.setId((long) stored.size() + 1);
        day.setTimesheetEmployeeProject(assignment);
        day.setWorkDate(date(number));
        day.setScheduledHours(new BigDecimal(hours));
        stored.put(key(day), day);
        return day;
    }

    private static String key(TimesheetEmployeeProjectDay day) {
        return day.getTimesheetEmployeeProject().getId() + ":" + day.getWorkDate();
    }

    private static LocalDate date(int day) { return LocalDate.of(2026, 9, day); }

    private static TimesheetScheduleDateRequest schedule(int day, String hours) {
        TimesheetScheduleDateRequest request = new TimesheetScheduleDateRequest();
        request.setWorkDate(date(day));
        request.setScheduledHours(new BigDecimal(hours));
        return request;
    }

    private static TimesheetDeletedDateRequest deleted(int day) {
        TimesheetDeletedDateRequest request = new TimesheetDeletedDateRequest();
        request.setWorkDate(date(day));
        return request;
    }

    private static TimesheetEmployeeProjectRequest request(TimesheetScheduleDateRequest... dates) {
        TimesheetEmployeeProjectRequest request = new TimesheetEmployeeProjectRequest();
        request.setScheduleDates(List.of(dates));
        return request;
    }

    private static TimesheetEmployeeProject project(Long id) {
        Employee employee = new Employee(); employee.setId(10L);
        Sow sow = new Sow(); sow.setId(20L);
        SowMilestone milestone = new SowMilestone(); milestone.setId(30L);
        TimesheetEmployeeProject project = new TimesheetEmployeeProject();
        project.setId(id); project.setEmployee(employee); project.setSow(sow); project.setMilestone(milestone);
        project.setStartDate(date(1)); project.setEndDate(date(30));
        return project;
    }
}
