package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetEntriesRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetEntryServiceTest {
    final TimesheetRepository repository = mock(TimesheetRepository.class);
    final HolidayRepository holidays = mock(HolidayRepository.class);
    final TimesheetEntryService service = new TimesheetEntryService(repository, holidays,
            Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC));
    final LocalDate monday = LocalDate.of(2026, 9, 14);
    final TimesheetEmployeeProject setup = TimesheetEmployeeProject.builder()
            .startDate(monday.minusDays(1)).endDate(monday.plusDays(5)).build();
    final Timesheet sheet = Timesheet.builder().id(1L).timesheetEmployeeProject(setup)
            .weekStartDate(monday.minusDays(1)).weekEndDate(monday.plusDays(5)).build();

    TimesheetEntryServiceTest() {
        setup.setLevel1Approver(employee(2L));
        setup.setLevel2Approver(employee(3L));
        for (int i = 0; i < 5; i++) setup.getDailySchedules().add(
                TimesheetEmployeeProjectDay.builder().workDate(monday.plusDays(i)).build());
        when(repository.findForEntryUpdate(1L)).thenReturn(Optional.of(sheet));
    }
    private Employee employee(Long id) {
        var employee = new Employee();
        employee.setId(id);
        return employee;
    }
    TimesheetEntriesRequest.Entry entry(int offset, TimesheetEntryType type, String hours, Long holiday) {
        return new TimesheetEntriesRequest.Entry(monday.plusDays(offset), type, new BigDecimal(hours), holiday, null);
    }
    void save(TimesheetEntriesRequest.Entry... entries) {
        service.save(1L, new TimesheetEntriesRequest(List.of(entries)));
    }
    @Test void createsSampleAndUpdatesWithoutDuplicatingOrRemovingOtherDays() {
        when(holidays.findById(9L)).thenReturn(Optional.of(Holiday.builder().id(9L)
                .holidayDate(monday.plusDays(2)).build()));
        save(entry(0, TimesheetEntryType.REGULAR, "8", null), entry(1, TimesheetEntryType.REGULAR, "8", null),
                entry(2, TimesheetEntryType.HOLIDAY, "8", 9L), entry(3, TimesheetEntryType.REGULAR, "8", null),
                entry(4, TimesheetEntryType.REGULAR, "8", null));
        assertThat(sheet.getRegularHours()).isEqualByComparingTo("32");
        assertThat(sheet.getHolidayHours()).isEqualByComparingTo("8");
        assertThat(sheet.getTotalHours()).isEqualByComparingTo("40");
        save(entry(0, TimesheetEntryType.REGULAR, "6", null));
        save(entry(0, TimesheetEntryType.REGULAR, "6", null));
        assertThat(sheet.getEntries()).hasSize(5);
        assertThat(sheet.getTotalHours()).isEqualByComparingTo("38");
    }
    @Test void savesAndSubmitsEntriesAndPreventsFurtherChanges() {
        var response = service.save(1L, new TimesheetEntriesRequest(List.of(
                entry(0, TimesheetEntryType.REGULAR, "8", null),
                entry(1, TimesheetEntryType.REGULAR, "8", null),
                entry(2, TimesheetEntryType.REGULAR, "8", null),
                entry(3, TimesheetEntryType.REGULAR, "8", null)), TimesheetStatus.SUBMITTED));
        assertThat(response.status()).isEqualTo(TimesheetStatus.SUBMITTED);
        assertThat(response.totalHours()).isEqualByComparingTo("32");
        assertThat(sheet.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 9, 19, 12, 0));
        assertThat(sheet.getApprovals()).hasSize(2);
        assertThat(sheet.getApprovals()).extracting(TimesheetApproval::getApprovalLevel).containsExactly(1, 2);
        assertThat(sheet.getApprovals()).extracting(a -> a.getApproverEmployee().getId()).containsExactly(2L, 3L);
        assertThat(sheet.getApprovals()).allSatisfy(approval -> {
            assertThat(approval.getTimesheet()).isSameAs(sheet);
            assertThat(approval.getStatus()).isEqualTo(TimesheetApprovalStatus.PENDING);
            assertThat(approval.getActionAt()).isNull();
            assertThat(approval.getComments()).isNull();
        });
        verify(repository).saveAndFlush(sheet);
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "6", null)))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.save(1L, new TimesheetEntriesRequest(List.of(
                entry(0, TimesheetEntryType.REGULAR, "6", null)), TimesheetStatus.SUBMITTED)))
                .isInstanceOf(InvalidOperationException.class);
        assertThat(sheet.getTotalHours()).isEqualByComparingTo("32");
        assertThat(sheet.getApprovals()).hasSize(2);
    }
    @Test void rejectsSubmissionWithoutBothApproversBeforeChangingEntries() {
        for (int missingLevel = 1; missingLevel <= 2; missingLevel++) {
            setup.setLevel1Approver(missingLevel == 1 ? null : employee(2L));
            setup.setLevel2Approver(missingLevel == 2 ? null : employee(3L));
            assertThatThrownBy(() -> service.save(1L, new TimesheetEntriesRequest(List.of(
                    entry(0, TimesheetEntryType.REGULAR, "8", null)), TimesheetStatus.SUBMITTED)))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessage("Configure both approval levels before submitting the timesheet");
        }
        assertThat(sheet.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(sheet.getSubmittedAt()).isNull();
        assertThat(sheet.getEntries()).isEmpty();
        assertThat(sheet.getApprovals()).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void omittedOrExplicitDraftStatusKeepsDraft() {
        setup.setLevel1Approver(null);
        setup.setLevel2Approver(null);
        save(entry(0, TimesheetEntryType.REGULAR, "8", null));
        assertThat(sheet.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(sheet.getSubmittedAt()).isNull();
        var response = service.save(1L, new TimesheetEntriesRequest(List.of(
                entry(0, TimesheetEntryType.REGULAR, "6", null)), TimesheetStatus.DRAFT));
        assertThat(response.status()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(sheet.getSubmittedAt()).isNull();
        assertThat(sheet.getApprovals()).isEmpty();
    }
    @Test void rejectsUnsupportedRequestedStatuses() {
        for (var status : TimesheetStatus.values()) {
            if (status == TimesheetStatus.DRAFT || status == TimesheetStatus.SUBMITTED) continue;
            assertThatThrownBy(() -> service.save(1L, new TimesheetEntriesRequest(List.of(
                    entry(0, TimesheetEntryType.REGULAR, "8", null)), status)))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessage("status must be DRAFT or SUBMITTED");
        }
        assertThat(sheet.getEntries()).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void invalidSubmissionLeavesDraftUnchanged() {
        assertThatThrownBy(() -> service.save(1L, new TimesheetEntriesRequest(List.of(
                entry(0, TimesheetEntryType.REGULAR, "8", null),
                entry(1, TimesheetEntryType.HOLIDAY, "8", null)), TimesheetStatus.SUBMITTED)))
                .isInstanceOf(InvalidOperationException.class);
        assertThat(sheet.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
        assertThat(sheet.getSubmittedAt()).isNull();
        assertThat(sheet.getEntries()).isEmpty();
        assertThat(sheet.getApprovals()).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void rejectsProtectedStatuses() {
        for (var status : TimesheetStatus.values()) {
            if (status == TimesheetStatus.DRAFT) continue;
            sheet.setStatus(status);
            assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "8", null)))
                    .isInstanceOf(InvalidOperationException.class);
        }
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void invalidBatchDoesNotChangeEarlierEntries() {
        save(entry(0, TimesheetEntryType.REGULAR, "8", null));
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "6", null),
                entry(1, TimesheetEntryType.HOLIDAY, "8", null))).isInstanceOf(InvalidOperationException.class);
        assertThat(sheet.getTotalHours()).isEqualByComparingTo("8");
        assertThat(sheet.getEntries().get(0).getHours()).isEqualByComparingTo("8");
    }
    @Test void rejectsLockedCancelledAndAfterEndDates() {
        setup.getDailySchedules().get(0).setLocked(true);
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "8", null)))
                .isInstanceOf(InvalidOperationException.class);
        setup.getDailySchedules().get(0).setLocked(false);
        setup.getDailySchedules().get(0).setStatus(TimesheetScheduleStatus.CANCELLED);
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "8", null)))
                .isInstanceOf(InvalidOperationException.class);
        setup.setAssignmentEndDate(monday);
        assertThatThrownBy(() -> save(entry(1, TimesheetEntryType.REGULAR, "8", null)))
                .isInstanceOf(InvalidOperationException.class);
    }
    @Test void validatesDailyTotalPrecisionAndDuplicateKeys() {
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "20", null),
                entry(0, TimesheetEntryType.LEAVE, "8", null))).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "8.001", null)))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.REGULAR, "8", null),
                entry(0, TimesheetEntryType.REGULAR, "7", null))).isInstanceOf(InvalidOperationException.class);
        assertThat(sheet.getEntries()).isEmpty();
    }
    @Test void validatesHolidayDateAndUnsupportedLeaveReference() {
        when(holidays.findById(9L)).thenReturn(Optional.of(Holiday.builder().id(9L).holidayDate(monday.plusDays(1)).build()));
        assertThatThrownBy(() -> save(entry(0, TimesheetEntryType.HOLIDAY, "8", 9L)))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> save(new TimesheetEntriesRequest.Entry(monday, TimesheetEntryType.LEAVE,
                BigDecimal.ONE, null, 9L))).isInstanceOf(InvalidOperationException.class);
    }
}
