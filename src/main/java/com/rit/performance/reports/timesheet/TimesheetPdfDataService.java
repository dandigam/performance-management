package com.rit.performance.reports.timesheet;

import com.rit.performance.entity.*;
import com.rit.performance.repository.TimesheetApprovalRepository;
import com.rit.performance.repository.TimesheetRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimesheetPdfDataService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final TimesheetRepository timesheets;
    private final TimesheetApprovalRepository approvals;
    private final UserRepository users;
    private final Clock clock;

    public TimesheetPdfDocument loadAuthorized(Long timesheetId, Authentication authentication) {
        Timesheet timesheet = timesheets.findForPdfById(timesheetId)
                .orElseThrow(TimesheetPdfNotFoundException::new);
        User user = currentUser(authentication);
        List<TimesheetApproval> history = approvals.findByTimesheetIdOrderByApprovalLevelAsc(timesheetId);
        verifyAccess(user, timesheet, history);
        verifyDownloadableStatus(timesheet.getStatus());

        List<TimesheetPdfDayHeader> headers = timesheet.getWeekStartDate().datesUntil(
                        timesheet.getWeekEndDate().plusDays(1))
                .map(date -> new TimesheetPdfDayHeader(date,
                        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US) + " " + date.getDayOfMonth()))
                .toList();
        List<TimesheetPdfDay> days = headers.stream().map(header -> mapDay(timesheet, header)).toList();
        TimesheetEmployeeProject setup = timesheet.getTimesheetEmployeeProject();
        Sow sow = setup == null ? firstSow(timesheet) : setup.getSow();
        String milestone = setup == null || setup.getMilestone() == null ? null : setup.getMilestone().getMilestoneName();
        String designation = null;
        if (setup != null && setup.getMilestonePositionAssignment() != null
                && setup.getMilestonePositionAssignment().getMilestonePosition() != null
                && setup.getMilestonePositionAssignment().getMilestonePosition().getPosition() != null) {
            designation = setup.getMilestonePositionAssignment().getMilestonePosition().getPosition().getName();
        }
        BigDecimal regular = sum(days, TimesheetPdfDay::regularHours);
        BigDecimal overtime = sum(days, TimesheetPdfDay::overtimeHours);
        TimesheetPdfProject project = new TimesheetPdfProject(
                setup == null ? (sow == null ? null : sow.getId()) : setup.getId(),
                sow == null ? (setup == null ? "General" : setup.getInternalWorkType()) : sow.getSowName(),
                sow == null ? null : sow.getSowName(), milestone, designation,
                sow == null || sow.getClient() == null ? null : sow.getClient().getClientName(),
                days, regular, overtime, sum(days, TimesheetPdfDay::totalHours));

        List<TimesheetPdfAuditEntry> audit = history.stream().map(this::mapApproval).toList();
        OffsetDateTime approvedAt = history.stream()
                .filter(a -> a.getStatus() == TimesheetApprovalStatus.APPROVED && a.getActionAt() != null)
                .map(TimesheetApproval::getActionAt).max(LocalDateTime::compareTo).map(this::atOffset).orElse(null);
        String comments = history.stream().filter(a -> a.getComments() != null && !a.getComments().isBlank())
                .reduce((first, second) -> second).map(TimesheetApproval::getComments).orElse(null);
        Employee employee = timesheet.getEmployee();
        return new TimesheetPdfDocument(timesheet.getId(), employee.getRitId(), employeeName(employee),
                employee.getWorkMode(), timesheet.getWeekStartDate(), timesheet.getWeekEndDate(),
                timesheet.getStatus().name(), statusLabel(timesheet.getStatus()), comments,
                value(timesheet.getRegularHours()), ZERO, value(timesheet.getHolidayHours()),
                value(timesheet.getLeaveHours()), value(timesheet.getTotalHours()),
                timesheet.getSubmittedAt() == null ? null : atOffset(timesheet.getSubmittedAt()), approvedAt,
                headers, List.of(project), audit, OffsetDateTime.now(clock));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new TimesheetPdfForbiddenException();
        }
        return users.findCurrentUser(principal.id()).orElseThrow(TimesheetPdfForbiddenException::new);
    }

    private void verifyAccess(User user, Timesheet timesheet, List<TimesheetApproval> history) {
        Long employeeId = user.getEmployee() == null ? null : user.getEmployee().getId();
        boolean owner = Objects.equals(employeeId, timesheet.getEmployee().getId());
        boolean approver = employeeId != null && history.stream()
                .anyMatch(a -> Objects.equals(employeeId, a.getApproverEmployee().getId()));
        TimesheetEmployeeProject setup = timesheet.getTimesheetEmployeeProject();
        approver = approver || employeeId != null && setup != null
                && ((setup.getLevel1Approver() != null && Objects.equals(employeeId, setup.getLevel1Approver().getId()))
                || (setup.getLevel2Approver() != null && Objects.equals(employeeId, setup.getLevel2Approver().getId())));
        String role = user.getRole() == null ? null : user.getRole().getCode();
        boolean privileged = "ADMIN".equalsIgnoreCase(role) || "HR".equalsIgnoreCase(role);
        if (!owner && !approver && !privileged) throw new TimesheetPdfForbiddenException();
    }

    private void verifyDownloadableStatus(TimesheetStatus status) {
        if (status == TimesheetStatus.DRAFT || status == TimesheetStatus.CANCELLED) {
            throw new TimesheetPdfUnavailableException();
        }
    }

    private TimesheetPdfDay mapDay(Timesheet timesheet, TimesheetPdfDayHeader header) {
        BigDecimal regular = entryHours(timesheet, header.date(), TimesheetEntryType.REGULAR);
        BigDecimal holiday = entryHours(timesheet, header.date(), TimesheetEntryType.HOLIDAY);
        BigDecimal leave = entryHours(timesheet, header.date(), TimesheetEntryType.LEAVE);
        return new TimesheetPdfDay(header.date(), header.label(), regular, ZERO, holiday, leave,
                regular.add(holiday).add(leave));
    }

    private BigDecimal entryHours(Timesheet timesheet, LocalDate date, TimesheetEntryType type) {
        return timesheet.getEntries().stream().filter(e -> date.equals(e.getWorkDate()) && e.getEntryType() == type)
                .map(TimesheetEntry::getHours).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
    }

    private TimesheetPdfAuditEntry mapApproval(TimesheetApproval approval) {
        return new TimesheetPdfAuditEntry(approval.getStatus().name(), "Level " + approval.getApprovalLevel(),
                approval.getApprovalLevel(), employeeName(approval.getApproverEmployee()),
                approval.getActionAt() == null ? null : atOffset(approval.getActionAt()), approval.getComments());
    }

    private Sow firstSow(Timesheet timesheet) {
        return timesheet.getEntries().stream().map(TimesheetEntry::getSow).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private BigDecimal sum(List<TimesheetPdfDay> days, java.util.function.Function<TimesheetPdfDay, BigDecimal> value) {
        return days.stream().map(value).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal value(BigDecimal value) { return value == null ? ZERO : value; }
    private OffsetDateTime atOffset(LocalDateTime value) { return value.atZone(clock.getZone()).toOffsetDateTime(); }
    private String employeeName(Employee employee) {
        return ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }
    private String statusLabel(TimesheetStatus status) {
        String raw = status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
