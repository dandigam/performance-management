package com.rit.performance.service;

import com.rit.performance.dto.request.EmployeeLeavePolicyRequest;
import com.rit.performance.dto.response.EmployeeLeavePolicyResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLeavePolicyService {
    private static final LocalDate LAST_MYSQL_DATE = LocalDate.of(9999, 12, 31);

    private final EmployeeLeavePolicyRepository assignments;
    private final EmployeeRepository employees;
    private final LeavePolicyRepository policies;

    @Transactional
    public EmployeeLeavePolicyResponse assign(Long employeeId, EmployeeLeavePolicyRequest request) {
        Employee employee = lockEmployee(employeeId);
        LeavePolicy policy = findPolicy(request.leavePolicyId());
        requireActivePolicy(policy);
        validatePeriod(request, policy);
        checkOverlap(employeeId, -1L, request.effectiveFrom(), request.effectiveTo());

        EmployeeLeavePolicy assignment = new EmployeeLeavePolicy();
        assignment.setEmployee(employee);
        apply(assignment, policy, request);
        return response(assignments.saveAndFlush(assignment));
    }

    @Transactional
    public EmployeeLeavePolicyResponse update(Long employeeId, Long assignmentId, EmployeeLeavePolicyRequest request) {
        lockEmployee(employeeId);
        EmployeeLeavePolicy assignment = findAssignment(employeeId, assignmentId);
        LeavePolicy policy = findPolicy(request.leavePolicyId());
        if (!policy.getId().equals(assignment.getLeavePolicy().getId())) requireActivePolicy(policy);
        validatePeriod(request, policy);
        if (assignment.getStatus() == LeavePolicyStatus.ACTIVE)
            checkOverlap(employeeId, assignmentId, request.effectiveFrom(), request.effectiveTo());
        apply(assignment, policy, request);
        return response(assignments.saveAndFlush(assignment));
    }

    public List<EmployeeLeavePolicyResponse> getForEmployee(Long employeeId) {
        findEmployee(employeeId);
        return assignments.findByEmployeeIdOrderByEffectiveFromDescIdDesc(employeeId).stream()
                .map(this::response).toList();
    }

    public EmployeeLeavePolicyResponse getCurrent(Long employeeId) {
        findEmployee(employeeId);
        return assignments.findCurrent(employeeId, LeavePolicyStatus.ACTIVE, LocalDate.now()).stream()
                .findFirst().map(this::response)
                .orElseThrow(() -> new ResourceNotFoundException("No current active leave policy assignment for employee: " + employeeId));
    }

    @Transactional
    public EmployeeLeavePolicyResponse changeStatus(Long employeeId, Long assignmentId, LeavePolicyStatus status) {
        lockEmployee(employeeId);
        EmployeeLeavePolicy assignment = findAssignment(employeeId, assignmentId);
        if (status == LeavePolicyStatus.ACTIVE) {
            requireActivePolicy(assignment.getLeavePolicy());
            checkOverlap(employeeId, assignmentId, assignment.getEffectiveFrom(), assignment.getEffectiveTo());
        }
        assignment.setStatus(status);
        return response(assignments.saveAndFlush(assignment));
    }

    private Employee findEmployee(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private Employee lockEmployee(Long id) {
        return employees.findByIdForLeavePolicyUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private LeavePolicy findPolicy(Long id) {
        return policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave policy not found: " + id));
    }

    private EmployeeLeavePolicy findAssignment(Long employeeId, Long id) {
        EmployeeLeavePolicy assignment = assignments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy assignment not found: " + id));
        if (!assignment.getEmployee().getId().equals(employeeId))
            throw new ResourceNotFoundException("Leave policy assignment not found: " + id);
        return assignment;
    }

    private void requireActivePolicy(LeavePolicy policy) {
        if (policy.getStatus() != LeavePolicyStatus.ACTIVE)
            throw new InvalidOperationException("Only an active leave policy can be assigned.");
    }

    private void validatePeriod(EmployeeLeavePolicyRequest request, LeavePolicy policy) {
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom()))
            throw new InvalidOperationException("effectiveTo must be on or after effectiveFrom.");
        if (request.effectiveFrom().isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null &&
                    (request.effectiveTo() == null || request.effectiveTo().isAfter(policy.getEffectiveTo()))))
            throw new InvalidOperationException("Assignment period must be within the leave policy effective period.");
    }

    private void checkOverlap(Long employeeId, Long excludedId, LocalDate from, LocalDate to) {
        if (assignments.countOverlaps(employeeId, LeavePolicyStatus.ACTIVE, excludedId,
                from, to == null ? LAST_MYSQL_DATE : to) > 0)
            throw new InvalidOperationException("An active leave policy assignment overlaps this period.");
    }

    private void apply(EmployeeLeavePolicy assignment, LeavePolicy policy, EmployeeLeavePolicyRequest request) {
        assignment.setLeavePolicy(policy);
        assignment.setEffectiveFrom(request.effectiveFrom());
        assignment.setEffectiveTo(request.effectiveTo());
    }

    private EmployeeLeavePolicyResponse response(EmployeeLeavePolicy assignment) {
        return new EmployeeLeavePolicyResponse(assignment.getId(), assignment.getEmployee().getId(),
                assignment.getLeavePolicy().getId(), assignment.getLeavePolicy().getPolicyName(),
                assignment.getEffectiveFrom(), assignment.getEffectiveTo(), assignment.getStatus(),
                assignment.getCreatedOn(), assignment.getCreatedBy(), assignment.getUpdatedOn(), assignment.getUpdatedBy());
    }
}
