package com.rit.performance.service.impl;

import com.rit.performance.dto.response.TimesheetSummaryResponse;
import com.rit.performance.dto.response.TimesheetWeekEntryResponse;
import com.rit.performance.dto.response.TimesheetWeekProjectResponse;
import com.rit.performance.dto.response.TimesheetWeekResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import com.rit.performance.entity.Timesheet;
import com.rit.performance.entity.TimesheetEntry;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.entity.TimesheetStatus;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectRepository;
import com.rit.performance.repository.TimesheetRepository;
import com.rit.performance.service.TimesheetGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rit.performance.entity.TimesheetEmployeeProjectDay;
import java.time.Clock;
import java.util.Collection;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetGenerationServiceImpl implements TimesheetGenerationService {
    private final TimesheetRepository timesheetRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void ensureWeeklyTimesheets(Long employeeId, Collection<LocalDate> scheduleDates) {
        if (scheduleDates == null || scheduleDates.stream().anyMatch(Objects::isNull))
            throw new InvalidOperationException("scheduleDates must contain valid work dates");
        if (scheduleDates.isEmpty()) return;
        employeeRepository.findById(employeeId).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found: " + employeeId));
        scheduleDates.stream().map(this::weekStart).distinct().sorted()
                .forEach(start -> timesheetRepository.insertWeeklyHeaderIfMissing(
                        employeeId, start, start.plusDays(6)));
    }

    @Override
    @Transactional
    public void cleanupEmptyDraftWeeks(Long employeeId, Collection<LocalDate> deletedDates) {
        if (deletedDates == null || deletedDates.stream().anyMatch(Objects::isNull))
            throw new InvalidOperationException("deletedDates must contain valid work dates");
        if (deletedDates.isEmpty()) return;
        employeeRepository.findById(employeeId).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found: " + employeeId));
        // Evaluate persisted state after the entire schedule batch, across all assignments.
        deletedDates.stream().map(this::weekStart).distinct().sorted()
                .forEach(start -> timesheetRepository.deleteEmptyDraftWeek(employeeId, start));
    }
    private final TimesheetEmployeeProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final SowMilestonePositionAssignmentRepository milestoneAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetSummaryResponse> getAll(Long employeeId, String status) {
        List<TimesheetStatus> statuses = tabStatuses(status);

        if (employeeId != null) {
            employeeRepository.findById(employeeId).orElseThrow(() ->
                    new ResourceNotFoundException("Employee not found: " + employeeId));
        }
        List<Timesheet> timesheets = timesheetRepository.findForStatusTab(
                employeeId, weekStart(LocalDate.now(clock)), statuses);

        Map<Long, List<TimesheetEmployeeProject>> projectsByEmployee = timesheets.stream()
                .map(timesheet -> timesheet.getEmployee().getId())
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        projectRepository::findByEmployeeIdOrderByStartDateDescIdDesc));

        return timesheets.stream()
                .map(timesheet -> summary(timesheet,
                        projectsByEmployee.getOrDefault(
                                timesheet.getEmployee().getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetWeekResponse getWeek(Long employeeId, LocalDate weekStart, Long timesheetId) {

        if (weekStart.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new InvalidOperationException("weekStart must be a Sunday");
        }
        if (weekStart.isAfter(weekStart(LocalDate.now(clock))))
            throw new InvalidOperationException("Future timesheet weeks cannot be fetched");
        LocalDate weekEnd = weekStart.plusDays(6);

        Timesheet timesheet = (timesheetId == null
                ? timesheetRepository.findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
                : timesheetRepository.findOneById(timesheetId))
                    .filter(item -> item.getEmployee().getId().equals(employeeId)
                            && item.getWeekStartDate().equals(weekStart))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Timesheet " + timesheetId + " does not match employee " + employeeId + " and week " + weekStart));

        List<TimesheetEmployeeProject> eligibleProjects = projectRepository.
                findByEmployeeIdOrderByStartDateDescIdDesc(employeeId)
                .stream()
                .filter(project -> !project.getStartDate().isAfter(weekEnd)
                        && (project.getEndDate() == null || !project.getEndDate().isBefore(weekStart)))
                .toList();

        List<SowMilestonePositionAssignment> milestoneAssignments = milestoneAssignmentRepository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(employeeId);

        List<TimesheetWeekProjectResponse> projects = eligibleProjects.stream()
                .map(project -> TimesheetWeekProjectResponse.builder()
                        .timesheetEmployeeProjectId(project.getId())
                        .sowId(project.getSow().getId())
                        .sowCode(project.getSow().getSowCode())
                        .sowName(project.getSow().getSowName())
                        .milestoneId(project.getMilestone() == null ? null : project.getMilestone().getId())
                        .milestoneName(project.getMilestone() == null ? null : project.getMilestone().getMilestoneName())
                        .scheduleDates(project.getDailySchedules().stream()
                                .filter(TimesheetEmployeeProjectDay::isActive)
                                .filter(day -> !day.getWorkDate().isBefore(weekStart)
                                        && !day.getWorkDate().isAfter(weekEnd))
                                .map(day -> com.rit.performance.dto.response.TimesheetDailyOverrideResponse.builder()
                                        .workDate(day.getWorkDate()).scheduledHours(day.getScheduledHours())
                                        .dayType(day.getDayType()).build()).toList())
                        .designationName(designationForWeek(milestoneAssignments,
                                project.getSow().getId(), weekStart, weekEnd))
                        .startDate(project.getStartDate())
                        .endDate(project.getEndDate())
                        .maxHoursPerDay(project.getMaxHoursPerDay())
                        .entries(timesheet.getEntries().stream()
                                .filter(entry -> entry.getSow() != null
                                        && entry.getSow().getId().equals(project.getSow().getId()))
                                .map(this::weekEntry)
                                .toList())
                        .build())
                .toList();

        String workMode = timesheet.getEmployee().getWorkMode();

        return TimesheetWeekResponse.builder()
                .timesheetId(timesheet.getId())
                .employeeId(employeeId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .workMode(workMode)
                .projects(projects)
                .status(timesheet.getStatus())
                .build();
    }

    private String designationForWeek(List<SowMilestonePositionAssignment> assignments,
                                      Long sowId, LocalDate weekStart, LocalDate weekEnd) {
        return assignments.stream()
                .filter(item -> item.getMilestonePosition().getSow().getId().equals(sowId))
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .filter(item -> !item.getAssignmentStartDate().isAfter(weekEnd)
                        && (item.getAssignmentEndDate() == null
                        || !item.getAssignmentEndDate().isBefore(weekStart)))
                .map(item -> item.getMilestonePosition().getPosition().getName())
                .findFirst()
                .orElse(null);
    }

    private TimesheetWeekEntryResponse weekEntry(TimesheetEntry entry) {
        return TimesheetWeekEntryResponse.builder()
                .entryId(entry.getId())
                .workDate(entry.getWorkDate())
                .entryType(entry.getEntryType())
                .hours(entry.getHours())
                .jobId(entry.getJobId())
                .sowId(entry.getSow() == null ? null : entry.getSow().getId())
                .leaveId(entry.getLeaveId())
                .holidayId(entry.getHoliday() == null ? null : entry.getHoliday().getId())
                .build();
    }

    private TimesheetSummaryResponse summary(
            Timesheet timesheet, List<TimesheetEmployeeProject> projects) {
        String clients = projects.stream()
                .filter(project -> !project.getStartDate().isAfter(timesheet.getWeekEndDate())
                        && (project.getEndDate() == null
                        || !project.getEndDate().isBefore(timesheet.getWeekStartDate())))
                .map(TimesheetEmployeeProject::getSow)
                .filter(Objects::nonNull)
                .map(sow -> sow.getClient() == null ? null : sow.getClient().getClientName())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        String comments = timesheet.getApprovals().stream()
                .map(approval -> approval.getComments())
                .filter(comment -> comment != null && !comment.isBlank())
                .collect(Collectors.joining("; "));
        BigDecimal regularHours = zero(timesheet.getRegularHours());
        BigDecimal holidayHours = zero(timesheet.getHolidayHours());
        BigDecimal leaveHours = zero(timesheet.getLeaveHours());
        return TimesheetSummaryResponse.builder()
                .timesheetId(timesheet.getId())
                .employeeId(timesheet.getEmployee().getId())
                .employeeName(employeeName(timesheet.getEmployee()))
                .periodStartDate(timesheet.getWeekStartDate())
                .periodEndDate(timesheet.getWeekEndDate())
                .status(timesheet.getStatus())
                .statusDisplay(statusDisplay(timesheet.getStatus()))
                .approvalStatus(approvalStatus(timesheet.getStatus()))
                .clientName(clients.isBlank() ? null : clients)
                .endClient(null)
                .totalHours(zero(timesheet.getTotalHours()))
                .regularHours(regularHours)
                .overtimeHours(BigDecimal.ZERO)
                .totalTimeOffHours(holidayHours.add(leaveHours))
                .file(null)
                .commentsNotes(comments.isBlank() ? null : comments)
                .build();
    }

    private String statusDisplay(TimesheetStatus status) {
        return switch (status) {
            case DRAFT -> "New / Not Submitted";
            case SUBMITTED -> "Submitted";
            case LEVEL1_APPROVED -> "Level 1 Approved";
            case REJECTED -> "Rejected";
            case APPROVED -> "Approved";
        };
    }

    private List<TimesheetStatus> tabStatuses(String status) {
        String tab = status == null ? "ALL" : status.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (tab) {
            case "NEW", "DRAFT" -> List.of(TimesheetStatus.DRAFT);
            case "PENDING" -> List.of(TimesheetStatus.SUBMITTED, TimesheetStatus.LEVEL1_APPROVED);
            case "REJECT", "REJECTED" -> List.of(TimesheetStatus.REJECTED);
            case "APPROVED" -> List.of(TimesheetStatus.APPROVED);
            case "SUBMITTED" -> List.of(TimesheetStatus.SUBMITTED);
            case "LEVEL1_APPROVED" -> List.of(TimesheetStatus.LEVEL1_APPROVED);
            case "ALL" -> List.of(TimesheetStatus.values());
            default -> throw new InvalidOperationException(
                    "Invalid status; use NEW, DRAFT, PENDING, REJECTED, APPROVED or ALL");
        };
    }

    private String approvalStatus(TimesheetStatus status) {
        return switch (status) {
            case DRAFT -> "Not Submitted";
            case SUBMITTED -> "Pending Level 1 Approval";
            case LEVEL1_APPROVED -> "Pending Level 2 Approval";
            case REJECTED -> "Rejected";
            case APPROVED -> "Approved";
        };
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }
}
