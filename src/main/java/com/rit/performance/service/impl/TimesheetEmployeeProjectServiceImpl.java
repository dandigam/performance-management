package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetEmployeeProjectResponse;
import com.rit.performance.dto.response.TimesheetEmployeeProjectSummaryResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.TimesheetEmployeeProjectRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.service.TimesheetEmployeeProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TimesheetEmployeeProjectServiceImpl implements TimesheetEmployeeProjectService {
    private final TimesheetEmployeeProjectRepository repository;
    private final EmployeeRepository employeeRepository;
    private final SowRepository sowRepository;
    private final SowMilestonePositionAssignmentRepository milestoneAssignmentRepository;
    private final SowMilestoneRepository milestoneRepository;
    private final TimesheetProjectScheduleService scheduleService;

    @Override
    public List<TimesheetEmployeeProjectResponse> create(
            Long employeeId, List<TimesheetEmployeeProjectRequest> requests) {
        validateBatch(requests);
        Employee employee = employee(employeeId);
        List<TimesheetEmployeeProject> assignments = requests.stream().map(request -> {
            TimesheetEmployeeProject assignment;
            if (request.getTimesheetEmployeeProjectId() == null) {
                assignment = new TimesheetEmployeeProject();
                assignment.setEmployee(employee);
            } else {
                assignment = repository.findById(request.getTimesheetEmployeeProjectId())
                        .filter(existing -> existing.getEmployee().getId().equals(employeeId))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Timesheet employee project not found: "
                                        + request.getTimesheetEmployeeProjectId()));
            }
            TimesheetEmployeeProject conflicting = repository
                    .findByEmployeeIdAndSowIdAndMilestoneId(
                            employeeId, request.getSowId(), request.getMilestoneId())
                    .filter(existing -> request.getTimesheetEmployeeProjectId() == null
                            || !existing.getId().equals(request.getTimesheetEmployeeProjectId()))
                    .orElse(null);
            if (conflicting != null) {
                throw new DuplicateResourceException(
                        "Timesheet project assignment already exists for employee "
                                + employeeId + ", SOW " + request.getSowId()
                                + " and start date " + request.getStartDate()
                                + "; use timesheetEmployeeProjectId "
                                + conflicting.getId() + " to update it");
            }
            apply(assignment, request);
            return assignment;
        }).toList();
        List<TimesheetEmployeeProject> saved = repository.saveAll(assignments);
        for (int i = 0; i < saved.size(); i++) scheduleService.regenerate(saved.get(i), requests.get(i));
        return saved.stream().map(this::response).toList();
    }

    @Override
    public List<TimesheetEmployeeProjectResponse> update(
            Long employeeId, List<TimesheetEmployeeProjectRequest> requests) {
        validateBatch(requests);
        employee(employeeId);
        List<TimesheetEmployeeProject> assignments = requests.stream().map(request -> {
            TimesheetEmployeeProject assignment = repository
                    .findByEmployeeIdAndSowIdAndMilestoneId(
                            employeeId, request.getSowId(), request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Timesheet project assignment not found for employee "
                                    + employeeId + ", SOW " + request.getSowId()
                                    + " and start date " + request.getStartDate()));
            apply(assignment, request);
            return assignment;
        }).toList();
        List<TimesheetEmployeeProject> saved = repository.saveAll(assignments);
        for (int i = 0; i < saved.size(); i++) scheduleService.regenerate(saved.get(i), requests.get(i));
        return saved.stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetEmployeeProjectSummaryResponse> getAll(Long employeeId) {
        employee(employeeId);
        return repository.findAllByEmployeeIdOrderByStartDateAscIdAsc(employeeId).stream()
                .map(this::summaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetEmployeeProjectResponse get(Long employeeId, Long sowId, Long milestoneId) {
        employee(employeeId);
        TimesheetEmployeeProject assignment = repository
                .findByEmployeeIdAndSowIdAndMilestoneId(employeeId, sowId, milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Timesheet project assignment not found for employee " + employeeId
                                + ", SOW " + sowId + " and milestone " + milestoneId));
        return response(assignment);
    }

    private void apply(TimesheetEmployeeProject assignment,
                       TimesheetEmployeeProjectRequest request) {
        validateDatesAndApprovers(assignment.getEmployee().getId(), request);
        assignment.setSow(sow(request.getSowId()));
        var milestone = milestoneRepository.findByIdAndSow_Id(request.getMilestoneId(), request.getSowId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found for SOW: " + request.getMilestoneId()));
        assignment.setMilestone(milestone);
        assignment.setStartDate(request.getStartDate().isAfter(milestone.getStartDate())
                ? request.getStartDate() : milestone.getStartDate());
        assignment.setEndDate(request.getEndDate().isBefore(milestone.getEndDate())
                ? request.getEndDate() : milestone.getEndDate());
        if (assignment.getEndDate().isBefore(assignment.getStartDate()))
            throw new InvalidOperationException("Assignment range does not overlap milestone range");
        assignment.setDefaultHoursPerDay(request.getDefaultHoursPerDay());
        assignment.setLevel1Approver(employee(request.getLevel1ApproverId()));
        assignment.setLevel2Approver(employee(request.getLevel2ApproverId()));
        assignment.setStatus(request.getStatus());
    }

    private void validateBatch(List<TimesheetEmployeeProjectRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidOperationException("At least one timesheet project is required");
        }
        Set<String> keys = new HashSet<>();
        for (TimesheetEmployeeProjectRequest request : requests) {
            String key = request.getSowId() + ":" + request.getMilestoneId();
            if (!keys.add(key)) {
                throw new DuplicateResourceException(
                        "Duplicate SOW and start date in request: " + key);
            }
        }
    }

    private void validateDatesAndApprovers(
            Long employeeId, TimesheetEmployeeProjectRequest request) {
        if (request.getDefaultHoursPerDay() == null
                && (request.getDailyOverrides() == null || request.getDailyOverrides().isEmpty())) {
            throw new InvalidOperationException(
                    "Provide defaultHoursPerDay or at least one daily override");
        }
        if (request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException("endDate cannot be before startDate");
        }
        if (request.getLevel1ApproverId().equals(request.getLevel2ApproverId())) {
            throw new InvalidOperationException(
                    "Level 1 and Level 2 approvers must be different employees");
        }
        if (employeeId.equals(request.getLevel1ApproverId())
                || employeeId.equals(request.getLevel2ApproverId())) {
            throw new InvalidOperationException(
                    "An employee cannot approve their own timesheet");
        }
    }

    private Employee employee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private Sow sow(Long id) {
        return sowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + id));
    }

    private TimesheetEmployeeProjectResponse response(TimesheetEmployeeProject assignment) {
        LocalDate weekStart = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        String designationName = milestoneAssignmentRepository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(
                        assignment.getEmployee().getId())
                .stream()
                .filter(item -> item.getMilestonePosition().getSow().getId()
                        .equals(assignment.getSow().getId()))
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .filter(item -> overlaps(item, weekStart, weekEnd))
                .map(item -> item.getMilestonePosition().getPosition().getName())
                .findFirst()
                .orElse(null);
        return TimesheetEmployeeProjectResponse.builder()
                .timesheetEmployeeProjectId(assignment.getId())
                .employeeId(assignment.getEmployee().getId())
                .employeeName(name(assignment.getEmployee()))
                .sowId(assignment.getSow().getId())
                .sowName(assignment.getSow().getSowName())
                .designationName(designationName)
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .milestoneId(assignment.getMilestone().getId())
                .milestoneName(assignment.getMilestone().getMilestoneName())
                .defaultHoursPerDay(assignment.getDefaultHoursPerDay())
                .dailyOverrides(scheduleService.overrides(assignment))
                .level1ApproverId(assignment.getLevel1Approver().getId())
                .level1ApproverName(name(assignment.getLevel1Approver()))
                .level2ApproverId(assignment.getLevel2Approver().getId())
                .level2ApproverName(name(assignment.getLevel2Approver()))
                .status(assignment.getStatus())
                .createdOn(assignment.getCreatedOn())
                .updatedOn(assignment.getUpdatedOn())
                .build();
    }

    private TimesheetEmployeeProjectSummaryResponse summaryResponse(
            TimesheetEmployeeProject assignment) {
        return TimesheetEmployeeProjectSummaryResponse.builder()
                .timesheetEmployeeProjectId(assignment.getId())
                .employeeId(assignment.getEmployee().getId())
                .sowId(assignment.getSow().getId())
                .sowName(assignment.getSow().getSowName())
                .milestoneId(assignment.getMilestone().getId())
                .milestoneName(assignment.getMilestone().getMilestoneName())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .defaultHoursPerDay(assignment.getDefaultHoursPerDay())
                .level1ApproverId(assignment.getLevel1Approver().getId())
                .level1ApproverName(name(assignment.getLevel1Approver()))
                .level2ApproverId(assignment.getLevel2Approver().getId())
                .level2ApproverName(name(assignment.getLevel2Approver()))
                .status(assignment.getStatus())
                .build();
    }

    private boolean overlaps(SowMilestonePositionAssignment assignment,
                             LocalDate weekStart, LocalDate weekEnd) {
        return !assignment.getAssignmentStartDate().isAfter(weekEnd)
                && (assignment.getAssignmentEndDate() == null
                || !assignment.getAssignmentEndDate().isBefore(weekStart));
    }

    private String name(Employee employee) {
        return (employee.getFirstName() + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }
}
