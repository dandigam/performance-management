package com.rit.performance.reports.timesheet;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Timesheet;
import com.rit.performance.entity.TimesheetStatus;
import com.rit.performance.entity.User;
import com.rit.performance.repository.TimesheetApprovalRepository;
import com.rit.performance.repository.TimesheetRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TimesheetPdfDataServiceTest {
    @Test
    void draftCannotBeDownloadedEvenByOwner() {
        TimesheetRepository timesheets = mock(TimesheetRepository.class);
        TimesheetApprovalRepository approvals = mock(TimesheetApprovalRepository.class);
        UserRepository users = mock(UserRepository.class);
        Employee employee = new Employee();
        employee.setId(2L);
        Timesheet timesheet = new Timesheet();
        timesheet.setId(125L);
        timesheet.setEmployee(employee);
        timesheet.setStatus(TimesheetStatus.DRAFT);
        User user = new User();
        user.setId(12L);
        user.setEmployee(employee);
        AuthenticatedUser principal = new AuthenticatedUser(12L, "employee@example.com", "", true, 0, List.of());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(timesheets.findForPdfById(125L)).thenReturn(Optional.of(timesheet));
        when(approvals.findByTimesheetIdOrderByApprovalLevelAsc(125L)).thenReturn(List.of());
        when(users.findCurrentUser(12L)).thenReturn(Optional.of(user));

        TimesheetPdfDataService service = new TimesheetPdfDataService(timesheets, approvals, users,
                Clock.systemUTC());

        assertThrows(TimesheetPdfUnavailableException.class,
                () -> service.loadAuthorized(125L, authentication));
    }
}
