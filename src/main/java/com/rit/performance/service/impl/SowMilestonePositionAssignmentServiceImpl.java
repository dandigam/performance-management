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

    @Override
    public SowMilestonePositionAssignmentResponse create(Long sowId, Long milestoneId,
            Long milestonePositionId, SowMilestonePositionAssignmentRequest request) {
        SowMilestonePosition position = findPosition(sowId, milestoneId, milestonePositionId);
        EmployeeAssignment sowAssignment = requireSowAssignment(
                request.getEmployeeAssignmentId(), sowId);
        if (repository
                .existsByEmployeeAssignment_EmployeeIdAndMilestonePosition_Milestone_IdAndStatusIgnoreCase(
                        sowAssignment.getEmployeeId(), milestoneId, "ACTIVE")) {
            throw new DuplicateResourceException(
                    "The selected employee is already assigned to this milestone. "
                            + "Choose another employee.");
        }
        validateDates(position, request.getAssignmentStartDate(),
                request.getAssignmentEndDate(), request.getStatus());
        SowMilestonePositionAssignment assignment = new SowMilestonePositionAssignment();
        apply(assignment, position, sowAssignment, request);
        assignment.setCreatedBy(request.getUpdatedBy());
        return toResponse(repository.saveAndFlush(assignment), employeeMap(sowAssignment));
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
        return toResponse(repository.saveAndFlush(assignment), employeeMap(sowAssignment));
    }

    @Override
    public SowMilestonePositionAssignmentResponse unassign(Long sowId, Long milestoneId,
            Long milestonePositionId, Long id,
            SowMilestonePositionUnassignRequest request) {
        SowMilestonePosition position = findPosition(sowId, milestoneId, milestonePositionId);
        SowMilestonePositionAssignment assignment = findAssignment(id, milestonePositionId);
        if (!"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            throw new InvalidOperationException("Only an ACTIVE assignment can be unassigned");
        }
        markUnassigned(assignment, request.getAssignmentEndDate());
        assignment.setUpdatedBy(request.getUpdatedBy());
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        completeParentIfNoActiveMilestones(saved.getEmployeeAssignment(),
                request.getAssignmentEndDate(), request.getUpdatedBy());
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
        if (!"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            throw new InvalidOperationException("Only an ACTIVE assignment can be unassigned");
        }
        markUnassigned(assignment, request.getAssignmentEndDate());
        assignment.setUpdatedBy(request.getUpdatedBy());
        SowMilestonePositionAssignment saved = repository.saveAndFlush(assignment);
        completeParentIfNoActiveMilestones(saved.getEmployeeAssignment(),
                request.getAssignmentEndDate(), request.getUpdatedBy());
        return toResponse(saved,
                employeeMap(assignment.getEmployeeAssignment()));
    }

    private void completeParentIfNoActiveMilestones(EmployeeAssignment parent,
            LocalDate endDate, Long updatedBy) {
        if (repository.existsByEmployeeAssignment_IdAndStatusIgnoreCase(
                parent.getId(), "ACTIVE")) {
            return;
        }
        parent.setEffectiveTo(endDate);
        parent.setStatus("COMPLETED");
        parent.setIsPrimaryAssignment(false);
        parent.setUpdatedBy(updatedBy);
        employeeAssignmentRepository.save(parent);
    }

    private void markUnassigned(SowMilestonePositionAssignment assignment,
            LocalDate endDate) {
        assignment.setAssignmentEndDate(endDate);
        assignment.setStatus("COMPLETED");
    }

    private void apply(SowMilestonePositionAssignment assignment,
            SowMilestonePosition position, EmployeeAssignment sowAssignment,
            SowMilestonePositionAssignmentRequest request) {
        assignment.setMilestonePosition(position);
        assignment.setEmployeeAssignment(sowAssignment);
        assignment.setAllocationPercentage(request.getAllocationPercentage());
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
        if (!"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
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
        if ("ACTIVE".equals(normalizedStatus) && endDate != null) {
            throw new InvalidOperationException(
                    "assignmentEndDate must be null when status is ACTIVE");
        }
        if ("COMPLETED".equals(normalizedStatus) && endDate == null) {
            throw new InvalidOperationException(
                    "assignmentEndDate is required when status is COMPLETED");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "COMPLETED").contains(normalized)) {
            throw new InvalidOperationException("status must be ACTIVE or COMPLETED");
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
                .seniority(position.getSeniority())
                .rateCardId(position.getRateCard() == null ? null : position.getRateCard().getId())
                .allocationPercentage(assignment.getAllocationPercentage())
                .positionType(assignment.getPositionType())
                .assignmentStartDate(assignment.getAssignmentStartDate())
                .assignmentEndDate(assignment.getAssignmentEndDate())
                .status(assignment.getStatus())
                .createdDate(assignment.getCreatedOn())
                .updatedDate(assignment.getUpdatedOn())
                .build();
    }
}
