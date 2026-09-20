package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetEntriesRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetEntryLeaveTest {
    private final TimesheetRepository timesheets = mock(TimesheetRepository.class);
    private final HolidayRepository holidays = mock(HolidayRepository.class);
    private final LeaveRequestRepository leaves = mock(LeaveRequestRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC);
    private final TimesheetEntryService service = new TimesheetEntryService(timesheets, holidays, leaves, clock);
    private final LocalDate date = LocalDate.of(2026, 9, 24);

    private Timesheet sheet() {
        Employee employee = new Employee(); employee.setId(1L);
        TimesheetEmployeeProject setup = new TimesheetEmployeeProject();
        setup.setStartDate(LocalDate.of(2026, 9, 1));
        setup.setEndDate(LocalDate.of(2026, 9, 30));
        TimesheetEmployeeProjectDay day = new TimesheetEmployeeProjectDay();
        day.setWorkDate(date); day.setScheduledHours(new BigDecimal("8.00"));
        setup.getDailySchedules().add(day);
        Timesheet sheet = new Timesheet(); sheet.setId(9L); sheet.setEmployee(employee);
        sheet.setTimesheetEmployeeProject(setup);
        sheet.setWeekStartDate(LocalDate.of(2026, 9, 20));
        sheet.setWeekEndDate(LocalDate.of(2026, 9, 26));
        when(timesheets.findForEntryUpdate(9L)).thenReturn(Optional.of(sheet));
        return sheet;
    }

    private TimesheetEntriesRequest request(BigDecimal hours, TimesheetEntryType type, Long leaveId) {
        return new TimesheetEntriesRequest(List.of(
                new TimesheetEntriesRequest.Entry(date, type, hours, null, leaveId)), TimesheetStatus.DRAFT);
    }

    private LeaveRequest approvedLeave() {
        LeaveRequest leave = new LeaveRequest(); leave.setId(2L); leave.setStatus(LeaveRequestStatus.APPROVED);
        LeaveRequestDay day = new LeaveRequestDay(); day.setLeaveDate(date);
        day.setRequestedHours(new BigDecimal("8.00")); leave.getDays().add(day);
        when(leaves.findByIdAndEmployeeId(2L, 1L)).thenReturn(Optional.of(leave));
        return leave;
    }

    @Test void savesApprovedLeaveReferenceAndHours() {
        Timesheet sheet = sheet(); approvedLeave();
        var result = service.save(9L, request(new BigDecimal("8.00"), TimesheetEntryType.LEAVE, 2L));
        assertEquals(2L, sheet.getEntries().get(0).getLeaveId());
        assertEquals(0, new BigDecimal("8.00").compareTo(result.leaveHours()));
        assertEquals(2L, result.entries().get(0).getLeaveId());
    }

    @Test void rejectsUnapprovedWrongEmployeeOrExcessHours() {
        sheet(); LeaveRequest leave = approvedLeave();
        leave.setStatus(LeaveRequestStatus.SUBMITTED);
        assertThrows(InvalidOperationException.class,
                () -> service.save(9L, request(new BigDecimal("8.00"), TimesheetEntryType.LEAVE, 2L)));
        leave.setStatus(LeaveRequestStatus.APPROVED);
        assertThrows(InvalidOperationException.class,
                () -> service.save(9L, request(new BigDecimal("9.00"), TimesheetEntryType.LEAVE, 2L)));
        when(leaves.findByIdAndEmployeeId(2L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.save(9L, request(new BigDecimal("8.00"), TimesheetEntryType.LEAVE, 2L)));
        assertThrows(InvalidOperationException.class,
                () -> service.save(9L, request(new BigDecimal("8.00"), TimesheetEntryType.REGULAR, 2L)));
    }
}
