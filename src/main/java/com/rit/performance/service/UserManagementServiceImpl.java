package com.rit.performance.service;

import com.rit.performance.dto.UserManagementUserResponse;
import com.rit.performance.dto.UserCreateRequest;
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
import com.rit.performance.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final UserRepository userRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowRepository sowRepository;
    private final LookupValueRepository lookupValueRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInvitationService invitationService;

    @Override
    @Transactional(readOnly = true)
    public List<UserManagementUserResponse> getUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(this::responseForUser).toList();
    }

    @Override
    @Transactional
    public UserManagementUserResponse createUser(UserCreateRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new InvalidOperationException("A user with this username already exists");
        }

        LookupValue role = activeSystemRole(request.roleId());
        Employee employee = null;
        if (request.employeeId() == null) {
            if (!username.equals(email)) {
                throw new InvalidOperationException(
                        "email must match username for a user without a linked employee");
            }
        } else {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employee not found: " + request.employeeId()));
            if (userRepository.findByEmployeeId(employee.getId()).isPresent()) {
                throw new InvalidOperationException("This employee already has a login account");
            }
            if (employee.getEmail() == null || !email.equalsIgnoreCase(employee.getEmail())) {
                throw new InvalidOperationException("email must match the linked employee email");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(role);
        user.setEmployee(employee);
        user.setStatus("INVITED");
        if (request.portalAccess() != null) {
            user.setPortalAccess(request.portalAccess());
        }
        user.setSessionVersion(0L);
        User saved = userRepository.saveAndFlush(user);
        if (employee != null) {
            synchronizeEmployeeRole(saved, role.getId());
        }
        if (request.sendInvitation()) {
            invitationService.send(saved, email);
        }
        return responseForUser(saved);
    }

    @Override
    @Transactional
    public void sendLoginSetup(Long userId) {
        User user = userRepository.findForSecurityUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String status = normalizeStatus(user.getStatus());
        if (!"INVITED".equals(status) && !"ACTIVE".equals(status)) {
            throw new InvalidOperationException("Login setup is only available for invited or active users");
        }
        String recipient = user.getEmployee() == null
                ? user.getUsername() : user.getEmployee().getEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new InvalidOperationException("The user does not have an email address for login setup");
        }
        invitationService.send(user, recipient.trim());
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
        LookupValue role = activeSystemRole(roleId);

        if (user.getRole() != null && roleId.equals(user.getRole().getId())) {
            return responseForUser(user);
        }

        user.setRole(role);
        user.setSessionVersion(user.getSessionVersion() + 1);
        User saved = userRepository.save(user);
        synchronizeEmployeeRole(saved, roleId);
        return responseForUser(saved);
    }

    @Override
    @Transactional
    public void updatePortalAccess(Long userId, String portalAccess) {
        User user = userRepository.findForSecurityUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (portalAccess != null) {
            user.setPortalAccess(portalAccess);
            userRepository.save(user);
        }
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

    private LookupValue activeSystemRole(Long roleId) {
        LookupValue role = lookupValueRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("SYSTEM_ROLE lookup not found: " + roleId));
        if (role.getLookupType() == null
                || !"SYSTEM_ROLE".equalsIgnoreCase(role.getLookupType().getCode())
                || !role.isActive() || !role.getLookupType().isActive()) {
            throw new InvalidOperationException("Lookup " + roleId + " is not an active SYSTEM_ROLE");
        }
        return role;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private UserManagementUserResponse responseForUser(User user) {
        Employee employee = user.getEmployee();
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
                .status(user.getStatus())
                .portalAccess(user.getPortalAccess())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedOn())
                .build();
    }

}
