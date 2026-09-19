package com.rit.performance.service.impl;

import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetEmployeeProjectResponse;
import com.rit.performance.dto.response.TimesheetEmployeeProjectSummaryResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.entity.TimesheetWorkType;
import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import java.util.Objects;
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
    private final com.rit.performance.service.TimesheetGenerationService generationService;

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
            apply(assignment, request);
            TimesheetEmployeeProject conflicting = (assignment.getMilestonePositionAssignment() == null
                    ? java.util.Optional.<TimesheetEmployeeProject>empty()
                    : repository.findByMilestonePositionAssignment_Id(assignment.getMilestonePositionAssignment().getId()))
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
            return assignment;
        }).toList();
        List<TimesheetEmployeeProject> saved = repository.saveAll(assignments);
        for (int i = 0; i < saved.size(); i++) scheduleService.applyChanges(saved.get(i), requests.get(i));
        generationService.ensureWeeklyTimesheets(employeeId, requests.stream()
                .flatMap(request -> request.getScheduleDates().stream())
                .map(date -> date.getWorkDate()).toList());
        generationService.cleanupEmptyDraftWeeks(employeeId, requests.stream()
                .flatMap(request -> request.getDeletedDates().stream())
                .map(date -> date.getWorkDate()).toList());
        return saved.stream().map(this::response).toList();
    }

    @Override
    public List<TimesheetEmployeeProjectResponse> update(
            Long employeeId, List<TimesheetEmployeeProjectRequest> requests) {
        validateBatch(requests);
        employee(employeeId);
        List<TimesheetEmployeeProject> assignments = requests.stream().map(request -> {
            TimesheetEmployeeProject assignment = (request.getTimesheetEmployeeProjectId() != null
                    ? repository.findById(request.getTimesheetEmployeeProjectId())
                    : request.getMilestonePositionAssignmentId() != null
                        ? repository.findByMilestonePositionAssignment_Id(request.getMilestonePositionAssignmentId())
                        : java.util.Optional.ofNullable(uniqueLegacySetup(employeeId, request.getSowId(), request.getMilestoneId())))
                    .filter(item -> item.getEmployee().getId().equals(employeeId))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Timesheet project assignment not found for employee "
                                    + employeeId + ", SOW " + request.getSowId()
                                    + " and start date " + request.getStartDate()));
            apply(assignment, request);
            return assignment;
        }).toList();
        List<TimesheetEmployeeProject> saved = repository.saveAll(assignments);
        for (int i = 0; i < saved.size(); i++) scheduleService.applyChanges(saved.get(i), requests.get(i));
        generationService.ensureWeeklyTimesheets(employeeId, requests.stream()
                .flatMap(request -> request.getScheduleDates().stream())
                .map(date -> date.getWorkDate()).toList());
        generationService.cleanupEmptyDraftWeeks(employeeId, requests.stream()
                .flatMap(request -> request.getDeletedDates().stream())
                .map(date -> date.getWorkDate()).toList());
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
        TimesheetEmployeeProject assignment = uniqueLegacySetup(employeeId, sowId, milestoneId);
        if (assignment == null) throw new ResourceNotFoundException("Timesheet setup not found");
        return response(assignment);
    }

    private TimesheetEmployeeProject uniqueLegacySetup(Long employeeId, Long sowId, Long milestoneId) {
        var matches = repository.findAllByEmployeeIdAndSowIdAndMilestoneId(employeeId, sowId, milestoneId);
        if (matches.size() > 1) throw new InvalidOperationException("Multiple assignment periods exist; use timesheetEmployeeProjectId");
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetEmployeeProjectResponse getBySetupId(Long employeeId, Long setupId) {
        return response(repository.findById(setupId).filter(item -> item.getEmployee().getId().equals(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet setup not found for employee")));
    }

    private void apply(TimesheetEmployeeProject assignment,
                       TimesheetEmployeeProjectRequest request) {
        validateDatesAndApprovers(assignment.getEmployee().getId(), request, assignment.getId() != null);
        if (assignment.getId() != null && (!Objects.equals(assignment.getSow() == null ? null : assignment.getSow().getId(), request.getSowId())
                || !Objects.equals(assignment.getMilestone() == null ? null : assignment.getMilestone().getId(), request.getMilestoneId())
                || assignment.getWorkType() != request.getWorkType()
                || (request.getTimesheetEmployeeProjectId() != null
                && !assignment.getId().equals(request.getTimesheetEmployeeProjectId()))))
            throw new InvalidOperationException("Assignment ID must match the employee, SOW and milestone configuration");
        if (assignment.getStatus() == TimesheetEmployeeProjectStatus.COMPLETED && assignment.getId() != null)
            throw new InvalidOperationException("Completed setup cannot be modified; create a new setup for the new assignment");
        assignment.setWorkType(request.getWorkType());
        if (request.getWorkType() == TimesheetWorkType.INTERNAL) {
            if (request.getSowId() != null || request.getMilestoneId() != null || request.getMilestonePositionAssignmentId() != null)
                throw new InvalidOperationException("Internal work must not reference a SOW, milestone or resource assignment");
            if (request.getInternalWorkType() == null || request.getInternalWorkType().isBlank())
                throw new InvalidOperationException("internalWorkType is required for internal work");
            assignment.setInternalWorkType(request.getInternalWorkType().trim());
            assignment.setStartDate(request.getStartDate());
            assignment.setEndDate(request.getEndDate());
            if (assignment.getAssignmentStartDate() == null)
                assignment.setAssignmentStartDate(request.getAssignmentStartDate() == null ? request.getStartDate() : request.getAssignmentStartDate());
            assignment.setAssignmentEndDate(request.getAssignmentEndDate());
            if (request.getStatus() == TimesheetEmployeeProjectStatus.COMPLETED && request.getAssignmentEndDate() == null
                    || request.getStatus() != TimesheetEmployeeProjectStatus.COMPLETED && request.getAssignmentEndDate() != null)
                throw new InvalidOperationException("assignmentEndDate is required only for COMPLETED internal work");
        } else {
        if (request.getSowId() == null || request.getMilestoneId() == null)
            throw new InvalidOperationException("sowId and milestoneId are required for project work");
        if (request.getInternalWorkType() != null && !request.getInternalWorkType().isBlank())
            throw new InvalidOperationException("internalWorkType must be empty for project work");
        Long linkId = request.getMilestonePositionAssignmentId();
        if (linkId == null && assignment.getMilestonePositionAssignment() != null)
            linkId = assignment.getMilestonePositionAssignment().getId();
        if (linkId == null) {
            var candidates = milestoneAssignmentRepository
                    .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(assignment.getEmployee().getId())
                    .stream().filter(item -> "ASSIGNED".equalsIgnoreCase(item.getStatus()))
                    .filter(item -> Objects.equals(item.getMilestonePosition().getSow().getId(), request.getSowId())
                            && Objects.equals(item.getMilestonePosition().getMilestone().getId(), request.getMilestoneId()))
                    .toList();
            if (candidates.size() == 1) linkId = candidates.get(0).getId();
        }
        if (linkId == null) throw new InvalidOperationException("milestonePositionAssignmentId is required for project work");
        var resource = milestoneAssignmentRepository.findOneById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone position assignment not found"));
        if (!Objects.equals(resource.getEmployeeAssignment().getEmployeeId(), assignment.getEmployee().getId())
                || !Objects.equals(resource.getMilestonePosition().getSow().getId(), request.getSowId())
                || !Objects.equals(resource.getMilestonePosition().getMilestone().getId(), request.getMilestoneId()))
            throw new InvalidOperationException("Resource assignment must match employee, SOW and milestone");
        if (assignment.getMilestonePositionAssignment() != null && !Objects.equals(assignment.getMilestonePositionAssignment().getId(), linkId))
            throw new InvalidOperationException("Cannot change the resource assignment of an existing setup");
        boolean completedResource = "COMPLETED".equalsIgnoreCase(resource.getStatus());
        if (!"ASSIGNED".equalsIgnoreCase(resource.getStatus()) && !completedResource)
            throw new InvalidOperationException("Timesheet setup requires an ASSIGNED resource");
        if (request.getAssignmentEndDate() != null || request.getStatus() == TimesheetEmployeeProjectStatus.COMPLETED)
            throw new InvalidOperationException("Complete project work through the resource unassign API");
        if (!completedResource && request.getAssignmentStartDate() != null
            && !request.getAssignmentStartDate().equals(resource.getAssignmentStartDate()))
            throw new InvalidOperationException("assignmentStartDate must match the linked resource assignment");
        assignment.setMilestonePositionAssignment(resource);
        assignment.setAssignmentStartDate(completedResource
            ? request.getAssignmentStartDate() == null
                ? request.getStartDate() : request.getAssignmentStartDate()
            : resource.getAssignmentStartDate());
        assignment.setAssignmentEndDate(null);
        assignment.setSow(sow(request.getSowId()));
        var milestone = milestoneRepository.findByIdAndSow_Id(request.getMilestoneId(), request.getSowId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found for SOW: " + request.getMilestoneId()));
        assignment.setMilestone(milestone);
        assignment.setStartDate(request.getStartDate().isAfter(milestone.getStartDate())
                ? request.getStartDate() : milestone.getStartDate());
        assignment.setEndDate(request.getEndDate().isBefore(milestone.getEndDate())
                ? request.getEndDate() : milestone.getEndDate());
        }
        if (assignment.getEndDate().isBefore(assignment.getStartDate()))
            throw new InvalidOperationException("Assignment range does not overlap milestone range");
        if (assignment.getEffectiveEndDate().isBefore(assignment.getEffectiveStartDate()))
            throw new InvalidOperationException("Planned setup does not overlap the assignment period");
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
        Set<Long> assignmentIds = new HashSet<>();
        for (TimesheetEmployeeProjectRequest request : requests) {
            if (request.getTimesheetEmployeeProjectId() != null
                    && !assignmentIds.add(request.getTimesheetEmployeeProjectId()))
                throw new DuplicateResourceException("Duplicate timesheet employee project ID in request");
            String key = request.getWorkType() == TimesheetWorkType.INTERNAL
                    ? "INTERNAL:" + request.getInternalWorkType() + ":" + request.getStartDate()
                    : "PROJECT:" + (request.getMilestonePositionAssignmentId() == null
                            ? request.getSowId() + ":" + request.getMilestoneId() : request.getMilestonePositionAssignmentId());
            if (!keys.add(key)) {
                throw new DuplicateResourceException(
                        "Duplicate SOW and start date in request: " + key);
            }
        }
    }

    private void validateDatesAndApprovers(
            Long employeeId, TimesheetEmployeeProjectRequest request, boolean updating) {
        if (request.getDailyOverrides() != null)
            throw new InvalidOperationException("dailyOverrides is no longer supported; use scheduleDates and deletedDates");
        if (request.getWorkType() == null || request.getStatus() == null || request.getStartDate() == null || request.getEndDate() == null)
            throw new InvalidOperationException("workType, status, plannedStartDate and plannedEndDate are required");
        if (request.getAssignmentEndDate() != null && request.getAssignmentStartDate() != null
                && request.getAssignmentEndDate().isBefore(request.getAssignmentStartDate()))
            throw new InvalidOperationException("assignmentEndDate cannot precede assignmentStartDate");
        if (request.getScheduleDates() == null || request.getDeletedDates() == null)
            throw new InvalidOperationException("scheduleDates and deletedDates must be arrays; use [] for no changes");
        if (!updating && !request.getDeletedDates().isEmpty())
            throw new InvalidOperationException("deletedDates must be empty when creating an assignment");
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
        String designationName = assignment.getSow() == null ? null : milestoneAssignmentRepository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(
                        assignment.getEmployee().getId())
                .stream()
                .filter(item -> item.getMilestonePosition().getSow().getId()
                        .equals(assignment.getSow().getId()))
                .filter(item -> ("ASSIGNED".equalsIgnoreCase(item.getStatus())))
                .filter(item -> overlaps(item, weekStart, weekEnd))
                .map(item -> item.getMilestonePosition().getPosition().getName())
                .findFirst()
                .orElse(null);
        return TimesheetEmployeeProjectResponse.builder()
                .timesheetEmployeeProjectId(assignment.getId())
                .employeeId(assignment.getEmployee().getId())
                .employeeName(name(assignment.getEmployee()))
                .sowId(assignment.getSow() == null ? null : assignment.getSow().getId())
                .sowName(assignment.getSow() == null ? null : assignment.getSow().getSowName())
                .designationName(designationName)
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .assignmentStartDate(assignment.getAssignmentStartDate()).assignmentEndDate(assignment.getAssignmentEndDate())
                .workType(assignment.getWorkType()).internalWorkType(assignment.getInternalWorkType())
                .milestonePositionAssignmentId(assignment.getMilestonePositionAssignment() == null ? null : assignment.getMilestonePositionAssignment().getId())
                .milestoneId(assignment.getMilestone() == null ? null : assignment.getMilestone().getId())
                .milestoneName(assignment.getMilestone() == null ? null : assignment.getMilestone().getMilestoneName())
                .defaultHoursPerDay(assignment.getDefaultHoursPerDay())
                .scheduleDates(scheduleService.overrides(assignment))
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
                .sowId(assignment.getSow() == null ? null : assignment.getSow().getId())
                .sowName(assignment.getSow() == null ? null : assignment.getSow().getSowName())
                .milestoneId(assignment.getMilestone() == null ? null : assignment.getMilestone().getId())
                .milestoneName(assignment.getMilestone() == null ? null : assignment.getMilestone().getMilestoneName())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .assignmentStartDate(assignment.getAssignmentStartDate()).assignmentEndDate(assignment.getAssignmentEndDate())
                .workType(assignment.getWorkType()).internalWorkType(assignment.getInternalWorkType())
                .milestonePositionAssignmentId(assignment.getMilestonePositionAssignment() == null ? null : assignment.getMilestonePositionAssignment().getId())
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
