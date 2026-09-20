package com.rit.performance.service;

import com.rit.performance.dto.UserManagementUserResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.EmployeeRole;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.User;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final UserRepository userRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowRepository sowRepository;
    private final LookupValueRepository lookupValueRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserManagementUserResponse> getUsers() {
        List<User> users = userRepository.findAllByOrderByIdAsc();
        List<Long> employeeIds = users.stream()
                .map(User::getEmployee)
                .filter(Objects::nonNull)
                .map(Employee::getId)
                .distinct()
                .toList();

        Map<Long, EmployeeAssignment> assignments = employeeIds.isEmpty()
                ? Map.of()
                : assignmentRepository.findCurrentForEmployees(employeeIds, LocalDate.now()).stream()
                        .collect(Collectors.toMap(EmployeeAssignment::getEmployeeId,
                                Function.identity(), (first, ignored) -> first, LinkedHashMap::new));

        List<Long> sowIds = assignments.values().stream()
                .map(EmployeeAssignment::getSowId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Sow> sows = sowRepository.findAllById(sowIds).stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));

        return users.stream().map(user -> toResponse(user, assignments, sows)).toList();
    }

    @Override
    @Transactional
    public UserManagementUserResponse updateStatus(Long userId, String requestedStatus) {
        User user = userRepository.findForSecurityUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String currentStatus = normalizeStatus(user.getStatus());
        String newStatus = normalizeStatus(requestedStatus);
        boolean allowed = ("ACTIVE".equals(currentStatus) && "INACTIVE".equals(newStatus))
                || (("INACTIVE".equals(currentStatus) || "LOCKED".equals(currentStatus))
                        && "ACTIVE".equals(newStatus));
        if (!allowed) {
            throw new InvalidOperationException(
                    "User status transition is not allowed: " + currentStatus + " -> " + newStatus);
        }

        user.setStatus(newStatus);
        user.setSessionVersion(user.getSessionVersion() + 1);
        User saved = userRepository.save(user);
        return responseForUser(saved);
    }

    @Override
    @Transactional
    public UserManagementUserResponse updateRole(Long userId, Long roleId) {
        User user = userRepository.findForSecurityUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        LookupValue role = lookupValueRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("SYSTEM_ROLE lookup not found: " + roleId));
        if (role.getLookupType() == null
                || !"SYSTEM_ROLE".equalsIgnoreCase(role.getLookupType().getCode())
                || !role.isActive() || !role.getLookupType().isActive()) {
            throw new InvalidOperationException("Lookup " + roleId + " is not an active SYSTEM_ROLE");
        }

        if (user.getRole() != null && roleId.equals(user.getRole().getId())) {
            return responseForUser(user);
        }

        user.setRole(role);
        user.setSessionVersion(user.getSessionVersion() + 1);
        User saved = userRepository.save(user);
        synchronizeEmployeeRole(saved, roleId);
        return responseForUser(saved);
    }

    private void synchronizeEmployeeRole(User user, Long roleId) {
        if (user.getEmployee() == null) return;
        Long employeeId = user.getEmployee().getId();
        LocalDate effectiveFrom = LocalDate.now();
        EmployeeRole current = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employeeId)
                .orElse(null);
        if (current != null && roleId.equals(current.getRoleId())) return;
        if (current != null) {
            current.setEffectiveTo(effectiveFrom);
            current.setIsCurrent(false);
            employeeRoleRepository.save(current);
        }
        EmployeeRole replacement = new EmployeeRole();
        replacement.setEmployeeId(employeeId);
        replacement.setRoleId(roleId);
        replacement.setEffectiveFrom(effectiveFrom);
        replacement.setIsCurrent(true);
        employeeRoleRepository.save(replacement);
    }

    private UserManagementUserResponse responseForUser(User user) {
        Employee employee = user.getEmployee();
        if (employee == null) return toResponse(user, Map.of(), Map.of());
        List<EmployeeAssignment> current = assignmentRepository.findCurrentForEmployees(
                List.of(employee.getId()), LocalDate.now());
        if (current.isEmpty() || current.get(0).getSowId() == null) {
            return toResponse(user, Map.of(), Map.of());
        }
        EmployeeAssignment assignment = current.get(0);
        Sow sow = sowRepository.findById(assignment.getSowId()).orElse(null);
        return toResponse(user, Map.of(employee.getId(), assignment),
                sow == null ? Map.of() : Map.of(sow.getId(), sow));
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private UserManagementUserResponse toResponse(User user,
            Map<Long, EmployeeAssignment> assignments, Map<Long, Sow> sows) {
        Employee employee = user.getEmployee();
        EmployeeAssignment assignment = employee == null ? null : assignments.get(employee.getId());
        Sow sow = assignment == null ? null : sows.get(assignment.getSowId());
        String departmentName = sow == null || sow.getBusinessUnit() == null
                ? null : sow.getBusinessUnit().getName();
        String employeeName = employee == null ? null
                : (employee.getFirstName() + " "
                        + (employee.getLastName() == null ? "" : employee.getLastName())).trim();

        return UserManagementUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(employee == null ? user.getUsername() : employee.getEmail())
                .employeeId(employee == null ? null : employee.getId())
                .employeeCode(employee == null ? null : employee.getRitId())
                .employeeName(employeeName)
                .roleId(user.getRole().getId())
                .roleCode(user.getRole().getCode())
                .roleName(user.getRole().getName())
                .departmentName(departmentName)
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedOn())
                .build();
    }
}
