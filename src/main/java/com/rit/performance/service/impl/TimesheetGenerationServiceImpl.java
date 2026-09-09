package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetGenerateRequest;
import com.rit.performance.dto.request.TimesheetHistoryGenerateRequest;
import com.rit.performance.dto.response.TimesheetGenerateResponse;
import com.rit.performance.dto.response.TimesheetSummaryResponse;
import com.rit.performance.dto.response.TimesheetWeekDayResponse;
import com.rit.performance.dto.response.TimesheetWeekEntryResponse;
import com.rit.performance.dto.response.TimesheetWeekProjectResponse;
import com.rit.performance.dto.response.TimesheetWeekResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Holiday;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import com.rit.performance.entity.Timesheet;
import com.rit.performance.entity.TimesheetEntry;
import com.rit.performance.entity.TimesheetEntryType;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import com.rit.performance.entity.TimesheetStatus;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.HolidayRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectRepository;
import com.rit.performance.repository.TimesheetRepository;
import com.rit.performance.service.TimesheetGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetGenerationServiceImpl implements TimesheetGenerationService {
    private final TimesheetRepository timesheetRepository;
    private final TimesheetEmployeeProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final SowMilestonePositionAssignmentRepository milestoneAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetSummaryResponse> getAll(Long employeeId) {
        List<Timesheet> timesheets = employeeId == null
                ? timesheetRepository.findAllByOrderByWeekStartDateDescIdDesc()
                : timesheetRepository.findByEmployeeIdOrderByWeekStartDateDescIdDesc(employeeId);
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
        LocalDate weekEnd = weekStart.plusDays(6);

        Timesheet timesheet = timesheetRepository.findOneById(timesheetId)
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
        List<Holiday> weekHolidays = workMode == null || workMode.isBlank()
                ? holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(
                        weekStart, weekEnd)
                : holidayRepository
                        .findByLocationTypeIgnoreCaseAndHolidayDateBetweenOrderByHolidayDateAsc(
                                workMode, weekStart, weekEnd);
        Map<LocalDate, Holiday> holidays = weekHolidays.stream()
                .filter(Holiday::isActive)
                .collect(Collectors.toMap(Holiday::getHolidayDate, Function.identity(),
                        (first, ignored) -> first));
        List<TimesheetWeekDayResponse> days = weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> {
                    Holiday holiday = holidays.get(date);
                    BigDecimal leaveHours = timesheet.getEntries().stream()
                            .filter(entry -> entry.getEntryType() == TimesheetEntryType.LEAVE)
                            .filter(entry -> date.equals(entry.getWorkDate()))
                            .map(TimesheetEntry::getHours)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return TimesheetWeekDayResponse.builder()
                            .date(date)
                            .holidayId(holiday == null ? null : holiday.getId())
                            .holidayName(holiday == null ? null : holiday.getHolidayName())
                            .leaveHours(leaveHours)
                            .build();
                })
                .toList();

        return TimesheetWeekResponse.builder()
                .timesheetId(timesheet.getId())
                .employeeId(employeeId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .workMode(workMode)
                .projects(projects)
                .days(days)
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

    @Override
    @Transactional
    public TimesheetGenerateResponse generate(TimesheetGenerateRequest request) {
        LocalDate weekStart = request.getPeriodStartDate();
        LocalDate weekEnd = request.getPeriodEndDate();
        if (!weekEnd.equals(weekStart.plusDays(6))) {
            throw new InvalidOperationException(
                    "periodEndDate must be six days after periodStartDate");
        }

        List<Long> employeeIds = projectRepository.findEmployeeIdsEligibleForTimesheetGeneration(weekStart, weekEnd);
        Map<Long, Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        List<Timesheet> generated = employeeIds.stream()
                .map(employeeId -> newTimesheet(employees.get(employeeId), weekStart, weekEnd))
                .toList();
        List<Timesheet> saved = timesheetRepository.saveAll(generated);

        return TimesheetGenerateResponse.builder()
                .periodStartDate(weekStart)
                .periodEndDate(weekEnd)
                .generatedCount(saved.size())
                .timesheetIds(saved.stream().map(Timesheet::getId).toList())
                .employeeIds(saved.stream().map(timesheet -> timesheet.getEmployee().getId()).toList())
                .build();
    }

    @Override
    @Transactional
    public String generatePreviousDatesTimesheets(TimesheetHistoryGenerateRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate accessStart = request.getAccessPeriodStartDate();
        if (!accessStart.isBefore(today)
                && request.getPreviousAccessPeriodStartDate() == null) {
            throw new InvalidOperationException(
                    "accessPeriodStartDate must be before today for historical generation");
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));

        LocalDate firstWeekStart = accessStart.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        LocalDate currentWeekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        LocalDate lastHistoricalWeekStart = currentWeekStart.minusWeeks(1);

        int removed = 0;
        if (request.getPreviousAccessPeriodStartDate() != null) {
            LocalDate previousWeekStart = request.getPreviousAccessPeriodStartDate().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            LocalDate deleteFrom = previousWeekStart.isBefore(firstWeekStart)
                    ? previousWeekStart : firstWeekStart;
            List<Timesheet> affected = deleteFrom.isAfter(lastHistoricalWeekStart)
                    ? List.of()
                    : timesheetRepository.findByEmployeeIdAndWeekStartDateBetween(
                            employee.getId(), deleteFrom, lastHistoricalWeekStart);
            removed = affected.size();
            timesheetRepository.deleteAll(affected);
            timesheetRepository.flush();
        }

        List<TimesheetEmployeeProject> projects = projectRepository.findByEmployeeIdOrderByStartDateDescIdDesc(request.getEmployeeId());

        Set<LocalDate> existingWeeks = firstWeekStart.isAfter(lastHistoricalWeekStart)
                ? Set.of()
                : new HashSet<>(timesheetRepository
                        .findByEmployeeIdAndWeekStartDateBetween(
                                request.getEmployeeId(), firstWeekStart,
                                lastHistoricalWeekStart)
                        .stream().map(Timesheet::getWeekStartDate).toList());

        List<Timesheet> pending = new ArrayList<>();
        int skippedExisting = 0;
        for (LocalDate weekStart = firstWeekStart;
             !weekStart.isAfter(lastHistoricalWeekStart);
             weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (!hasEligibleProject(projects, weekStart, weekEnd)) {
                continue;
            }
            if (existingWeeks.contains(weekStart)) {
                skippedExisting++;
                continue;
            }
            pending.add(newTimesheet(employee, weekStart, weekEnd));
        }

        List<Timesheet> saved = timesheetRepository.saveAll(pending);
        return "Historical timesheets updated successfully: " + removed
                + " removed, " + saved.size()
                + " created, " + skippedExisting + " already existed";
    }

    private boolean hasEligibleProject(List<TimesheetEmployeeProject> projects,
                                       LocalDate weekStart, LocalDate weekEnd) {
        return projects.stream().anyMatch(project ->
                project.getStatus() == TimesheetEmployeeProjectStatus.ACTIVE
                        && !project.getStartDate().isAfter(weekEnd)
                        && (project.getEndDate() == null
                        || !project.getEndDate().isBefore(weekStart)));
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

    private Timesheet newTimesheet(Employee employee, LocalDate weekStart, LocalDate weekEnd) {
        Timesheet timesheet = new Timesheet();
        timesheet.setEmployee(employee);
        timesheet.setWeekStartDate(weekStart);
        timesheet.setWeekEndDate(weekEnd);
        timesheet.setRegularHours(BigDecimal.ZERO);
        timesheet.setHolidayHours(BigDecimal.ZERO);
        timesheet.setLeaveHours(BigDecimal.ZERO);
        timesheet.setTotalHours(BigDecimal.ZERO);
        timesheet.setStatus(TimesheetStatus.DRAFT);
        return timesheet;
    }
}
