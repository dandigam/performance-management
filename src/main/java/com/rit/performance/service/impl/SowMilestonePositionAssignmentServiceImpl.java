package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowMilestonePositionAssignmentRequest;
import com.rit.performance.dto.request.SowMilestonePositionUnassignRequest;
import com.rit.performance.dto.request.SowAssignmentUnassignRequest;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowMilestonePositionAssignmentService;
import com.rit.performance.service.SowResourceRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SowMilestonePositionAssignmentServiceImpl
        implements SowMilestonePositionAssignmentService {
    private final SowMilestonePositionAssignmentRepository repository;
    private final SowMilestonePositionRepository positionRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SowResourceRequirementService resourceRequirementService;
    private final TimesheetEmployeeProjectRepository timesheetProjectRepository;
    private final TimesheetAssignmentCompletionService timesheetCompletionService;

    @Override
    public SowMilestonePositionAssignmentResponse create(Long sowId, Long milestoneId,
            Long milestonePositionId, SowMilestonePositionAssignmentRequest request) {

        SowMilestonePosition position = findPosition(sowId, milestoneId, milestonePositionId);


        EmployeeAssignment sowAssignment = requireSowAssignment(request.getEmployeeAssignmentId(), sowId);

        SowMilestonePositionAssignment assignment = new SowMilestonePositionAssignment();
        apply(assignment, position, sowAssignment, request);
        assignment.setCreatedBy(request.getUpdatedBy());
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        reconcilePositionStatus(position);
        resourceRequirementService.onResourceAssigned(sowId);
        return toResponse(saved, employeeMap(sowAssignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowMilestonePositionAssignmentResponse> getAll(Long sowId, Long milestoneId,
            Long milestonePositionId) {
        findPosition(sowId, milestoneId, milestonePositionId);
        List<SowMilestonePositionAssignment> assignments = repository
                .findByMilestonePosition_IdOrderByAssignmentStartDateDescIdDesc(
                        milestonePositionId);
        Map<Long, Employee> employees = employeeMap(assignments.stream()
                .map(SowMilestonePositionAssignment::getEmployeeAssignment).toList());
        return assignments.stream().map(value -> toResponse(value, employees)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowMilestonePositionAssignmentResponse> getByEmployeeId(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }
        List<SowMilestonePositionAssignment> assignments = repository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(
                        employeeId);
        Map<Long, Employee> employees = employeeMap(assignments.stream()
                .map(SowMilestonePositionAssignment::getEmployeeAssignment).toList());
        return assignments.stream().map(value -> toResponse(value, employees)).toList();
    }

    @Override
    public SowMilestonePositionAssignmentResponse update(Long sowId, Long milestoneId,
            Long milestonePositionId, Long id,
            SowMilestonePositionAssignmentRequest request) {
        SowMilestonePosition position = findPosition(sowId, milestoneId, milestonePositionId);
        SowMilestonePositionAssignment assignment = findAssignment(id, milestonePositionId);
        EmployeeAssignment sowAssignment = requireSowAssignment(
                request.getEmployeeAssignmentId(), sowId);
        validateDates(position, request.getAssignmentStartDate(),
                request.getAssignmentEndDate(), request.getStatus());
        apply(assignment, position, sowAssignment, request);
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        reconcilePositionStatus(position);
        reconcileParentStatus(sowAssignment, saved.getAssignmentEndDate(), request.getUpdatedBy());
        if ("COMPLETED".equalsIgnoreCase(saved.getStatus())) completeTimesheetSetup(saved);
        reconcileResourceRequirement(sowId, saved.getStatus());
        return toResponse(saved, employeeMap(sowAssignment));
    }

    @Override
    public SowMilestonePositionAssignmentResponse unassign(Long sowId, Long milestoneId,
            Long milestonePositionId, Long id,
            SowMilestonePositionUnassignRequest request) {
        SowMilestonePosition position = findPosition(sowId, milestoneId, milestonePositionId);
        SowMilestonePositionAssignment assignment = findAssignment(id, milestonePositionId);
        if (!isAssigned(assignment.getStatus())) {
            throw new InvalidOperationException("Only an ASSIGNED assignment can be unassigned");
        }
        endAssignment(assignment, request.getAssignmentEndDate(),
                request.getAssignmentStatus());
        assignment.setUpdatedBy(request.getUpdatedBy());
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        completeTimesheetSetup(saved);
        reconcilePositionStatus(saved.getMilestonePosition());
        reconcileParentStatus(saved.getEmployeeAssignment(),
                request.getAssignmentEndDate(), request.getUpdatedBy());
        reconcileResourceRequirement(sowId, saved.getStatus());
        return toResponse(saved,
                employeeMap(assignment.getEmployeeAssignment()));
    }

    @Override
    public SowMilestonePositionAssignmentResponse unassign(Long sowId, Long id,
            SowAssignmentUnassignRequest request) {
        SowMilestonePositionAssignment assignment = repository.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone position assignment not found: " + id));
        SowMilestonePosition position = assignment.getMilestonePosition();
        if (!Objects.equals(position.getSow().getId(), sowId)) {
            throw new InvalidOperationException(
                    "Assignment " + id + " does not belong to SOW " + sowId);
        }
        if (!isAssigned(assignment.getStatus())) {
            throw new InvalidOperationException("Only an ASSIGNED assignment can be unassigned");
        }
        endAssignment(assignment, request.getAssignmentEndDate(),
                request.getAssignmentStatus());
        assignment.setUpdatedBy(request.getUpdatedBy());
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        completeTimesheetSetup(saved);
        reconcilePositionStatus(saved.getMilestonePosition());
        reconcileParentStatus(saved.getEmployeeAssignment(),
                request.getAssignmentEndDate(), request.getUpdatedBy());
        reconcileResourceRequirement(sowId, saved.getStatus());
        return toResponse(saved,
                employeeMap(assignment.getEmployeeAssignment()));
    }

    private void completeTimesheetSetup(SowMilestonePositionAssignment assignment) {
        var position = assignment.getMilestonePosition();
        Long employeeId = assignment.getEmployeeAssignment().getEmployeeId();
        var linkedSetup = timesheetProjectRepository.findByMilestonePositionAssignment_Id(assignment.getId());
        if (linkedSetup.isPresent()) {
            completeSetup(linkedSetup.get(), assignment);
            return;
        }
        // The current setup is shared by all of this employee's roles in the same milestone.
        boolean stillAssigned = repository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(employeeId).stream()
                .anyMatch(other -> !Objects.equals(other.getId(), assignment.getId())
                        && isAssigned(other.getStatus())
                        && Objects.equals(other.getMilestonePosition().getSow().getId(), position.getSow().getId())
                        && Objects.equals(other.getMilestonePosition().getMilestone().getId(), position.getMilestone().getId()));
        if (stillAssigned) return;
        var legacy = timesheetProjectRepository.findAllByEmployeeIdAndSowIdAndMilestoneId(
                employeeId, position.getSow().getId(), position.getMilestone().getId()).stream()
                .filter(setup -> setup.getMilestonePositionAssignment() == null).toList();
        if (!legacy.isEmpty()) throw new InvalidOperationException(
                "Link the legacy timesheet setup to its milestone position assignment before completing it");
    }

    private void completeSetup(TimesheetEmployeeProject setup, SowMilestonePositionAssignment assignment) {
        timesheetCompletionService.cancelAfter(setup, assignment.getAssignmentEndDate(), assignment.getUpdatedBy());
        setup.setAssignmentStartDate(assignment.getAssignmentStartDate());
        setup.setAssignmentEndDate(assignment.getAssignmentEndDate());
        setup.setStatus(TimesheetEmployeeProjectStatus.COMPLETED);
        setup.setUpdatedBy(assignment.getUpdatedBy());
        timesheetProjectRepository.save(setup);
    }

    private void reconcileParentStatus(EmployeeAssignment parent,
            LocalDate endDate, Long updatedBy) {
        if (repository.existsByEmployeeAssignment_IdAndStatusIgnoreCase(
                parent.getId(), "ASSIGNED")) {
            return;
        }
        List<SowMilestonePositionAssignment> childAssignments = repository
                .findByEmployeeAssignment_IdOrderByAssignmentStartDateDescIdDesc(parent.getId());
        LocalDate latestEndDate = childAssignments.stream()
                .map(SowMilestonePositionAssignment::getAssignmentEndDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(endDate);
        parent.setEffectiveTo(latestEndDate);
        parent.setStatus("COMPLETED");

        parent.setUpdatedBy(updatedBy);
        employeeAssignmentRepository.save(parent);
    }

    private void endAssignment(SowMilestonePositionAssignment assignment,
            LocalDate endDate, String assignmentStatus) {
        if (endDate == null || endDate.isBefore(assignment.getAssignmentStartDate())) {
            throw new InvalidOperationException("assignmentEndDate must be on or after assignmentStartDate");
        }
        assignment.setAssignmentEndDate(endDate);
        assignment.setStatus(normalizeTerminalStatus(assignmentStatus));
    }

    private void reconcilePositionStatus(SowMilestonePosition position) {
        if (repository.existsByMilestonePosition_IdAndStatusIgnoreCase(
                position.getId(), "ASSIGNED")) {
            position.setStatus("ASSIGNED");
        } else {
            position.setStatus("OPEN");
        }
        positionRepository.saveAndFlush(position);
    }

    private void reconcileResourceRequirement(Long sowId, String assignmentStatus) {
        if ("COMPLETED".equalsIgnoreCase(assignmentStatus)) {
            resourceRequirementService.onResourceCompleted(sowId);
        } else {
            resourceRequirementService.onResourceUnassigned(sowId);
        }
    }

    private String normalizeTerminalStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COMPLETED", "UNASSIGNED").contains(normalized)) {
            throw new InvalidOperationException(
                    "assignmentStatus must be COMPLETED or UNASSIGNED");
        }
        return "COMPLETED";
    }

    private void apply(SowMilestonePositionAssignment assignment,
            SowMilestonePosition position, EmployeeAssignment sowAssignment,
            SowMilestonePositionAssignmentRequest request) {
        validateDates(position, request.getAssignmentStartDate(), request.getAssignmentEndDate(), request.getStatus());
        assignment.setMilestonePosition(position);
        assignment.setEmployeeAssignment(sowAssignment);
        assignment.setPositionType(normalizePositionType(request.getPositionType()));
        assignment.setAssignmentStartDate(request.getAssignmentStartDate());
        assignment.setAssignmentEndDate(request.getAssignmentEndDate());
        assignment.setStatus(normalizeStatus(request.getStatus()));
        assignment.setUpdatedBy(request.getUpdatedBy());
    }

    private SowMilestonePosition findPosition(Long sowId, Long milestoneId, Long positionId) {
        return positionRepository.findByIdAndMilestone_IdAndSow_Id(positionId, milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone position " + positionId + " not found for milestone "
                                + milestoneId + " and SOW " + sowId));
    }

    private SowMilestonePositionAssignment findAssignment(Long id, Long positionId) {
        SowMilestonePositionAssignment assignment = repository.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone position assignment not found: " + id));
        if (!Objects.equals(assignment.getMilestonePosition().getId(), positionId)) {
            throw new InvalidOperationException(
                    "Assignment does not belong to milestone position " + positionId);
        }
        return assignment;
    }

    private EmployeeAssignment requireSowAssignment(Long id, Long sowId) {
        EmployeeAssignment assignment = employeeAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee SOW assignment not found: " + id));
        if (!Objects.equals(assignment.getSowId(), sowId)) {
            throw new InvalidOperationException(
                    "Employee assignment does not belong to SOW " + sowId);
        }
        if (!isAssigned(assignment.getStatus())) {
            throw new InvalidOperationException("Employee SOW assignment is not active: " + id);
        }
        return assignment;
    }

    private void validateDates(SowMilestonePosition position, LocalDate startDate,
            LocalDate endDate, String status) {
        String normalizedStatus = normalizeStatus(status);
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidOperationException(
                    "assignmentEndDate cannot be before assignmentStartDate");
        }
        if (position.getStartDate() != null && startDate.isBefore(position.getStartDate())) {
            throw new InvalidOperationException(
                    "assignmentStartDate cannot be before the milestone position startDate");
        }
        if (position.getEndDate() != null && endDate != null
                && endDate.isAfter(position.getEndDate())) {
            throw new InvalidOperationException(
                    "assignmentEndDate cannot be after the milestone position endDate");
        }
        if ("ASSIGNED".equals(normalizedStatus) && endDate != null) {
            throw new InvalidOperationException(
                    "assignmentEndDate must be null when status is ASSIGNED");
        }
        if ("COMPLETED".equals(normalizedStatus) && endDate == null) {
            throw new InvalidOperationException(
                    "assignmentEndDate is required when status is COMPLETED");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ASSIGNED", "COMPLETED").contains(normalized)) {
            throw new InvalidOperationException("status must be ASSIGNED or COMPLETED");
        }
        return normalized;
    }

    private String normalizePositionType(String positionType) {
        String normalized = positionType.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        if ("NONBILLABLE".equals(normalized)) normalized = "NON_BILLABLE";
        if (!Set.of("BILLABLE", "NON_BILLABLE").contains(normalized)) {
            throw new InvalidOperationException(
                    "positionType must be BILLABLE or NON_BILLABLE");
        }
        return normalized;
    }

    private static boolean isAssigned(String status) {
        return "ASSIGNED".equalsIgnoreCase(status);
    }

    private Map<Long, Employee> employeeMap(EmployeeAssignment assignment) {
        return employeeMap(List.of(assignment));
    }

    private Map<Long, Employee> employeeMap(List<EmployeeAssignment> assignments) {
        List<Long> ids = assignments.stream().map(EmployeeAssignment::getEmployeeId)
                .filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : employeeRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    private SowMilestonePositionAssignmentResponse toResponse(
            SowMilestonePositionAssignment assignment, Map<Long, Employee> employees) {
        EmployeeAssignment sowAssignment = assignment.getEmployeeAssignment();
        SowMilestonePosition position = assignment.getMilestonePosition();
        Employee employee = employees.get(sowAssignment.getEmployeeId());
        String employeeName = employee == null ? null
                : ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return SowMilestonePositionAssignmentResponse.builder()
                .id(assignment.getId())
                .employeeAssignmentId(sowAssignment.getId())
                .employeeId(sowAssignment.getEmployeeId())
                .employeeName(employeeName)
                .sowId(position.getSow().getId())
                .milestoneId(position.getMilestone().getId())
                .milestonePositionId(position.getId())
                .positionId(position.getPosition().getId())
                .positionName(position.getPositionName())
                .seniorityId(position.getSeniority() == null
                        ? null : position.getSeniority().getId())
                .seniority(position.getSeniority() == null
                        ? null : position.getSeniority().getName())
                .rateCardId(position.getRateCard() == null ? null : position.getRateCard().getId())
                .positionType(assignment.getPositionType())
                .assignmentStartDate(assignment.getAssignmentStartDate())
                .assignmentEndDate(assignment.getAssignmentEndDate())
                .status(assignment.getStatus())
                .createdDate(assignment.getCreatedOn())
                .updatedDate(assignment.getUpdatedOn())
                .build();
    }
}
