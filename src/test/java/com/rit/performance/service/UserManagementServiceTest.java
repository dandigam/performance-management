package com.rit.performance.service;

import com.rit.performance.dto.UserManagementUserResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.LookupType;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.User;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class UserManagementServiceTest {

    @Test
    void returnsLoginAccountsIncludingUsersWithoutEmployees() {
        UserRepository userRepository = mock(UserRepository.class);
        EmployeeAssignmentRepository assignmentRepository = mock(EmployeeAssignmentRepository.class);
        SowRepository sowRepository = mock(SowRepository.class);
        UserManagementService service = new UserManagementServiceImpl(
                userRepository, assignmentRepository, sowRepository,
                mock(LookupValueRepository.class), mock(EmployeeRoleRepository.class));

        LookupValue managerRole = lookup(31L, "MANAGER", "Manager");
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setFirstName("Charan");
        employee.setLastName("Kovvuru");
        employee.setEmail("charan@gmail.com");
        employee.setRitId("RIT03");

        User linked = user(12L, "charan@gmail.com", managerRole, employee);
        linked.setLastLoginAt(LocalDateTime.of(2026, 9, 20, 9, 30));
        linked.setCreatedOn(LocalDateTime.of(2026, 1, 10, 12, 0));
        User unlinked = user(13L, "admin", lookup(1L, "ADMIN", "Administrator"), null);

        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployeeId(2L);
        assignment.setSowId(7L);
        Sow sow = new Sow();
        sow.setId(7L);
        sow.setBusinessUnit(lookup(8L, "ENGINEERING", "Engineering"));

        when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of(linked, unlinked));
        when(assignmentRepository.findCurrentForEmployees(eq(List.of(2L)), any(LocalDate.class)))
                .thenReturn(List.of(assignment));
        when(sowRepository.findAllById(List.of(7L))).thenReturn(List.of(sow));

        List<UserManagementUserResponse> result = service.getUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmployeeId()).isEqualTo(2L);
        assertThat(result.get(0).getEmployeeCode()).isEqualTo("RIT03");
        assertThat(result.get(0).getDepartmentName()).isEqualTo("Engineering");
        assertThat(result.get(0).getLastLoginAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 20, 9, 30));
        assertThat(result.get(1).getUsername()).isEqualTo("admin");
        assertThat(result.get(1).getEmail()).isEqualTo("admin");
        assertThat(result.get(1).getEmployeeId()).isNull();
        assertThat(result.get(1).getEmployeeName()).isNull();
    }

    @Test
    void unlocksLockedUserAndInvalidatesExistingSessions() {
        UserRepository userRepository = mock(UserRepository.class);
        EmployeeAssignmentRepository assignmentRepository = mock(EmployeeAssignmentRepository.class);
        SowRepository sowRepository = mock(SowRepository.class);
        UserManagementService service = new UserManagementServiceImpl(
                userRepository, assignmentRepository, sowRepository,
                mock(LookupValueRepository.class), mock(EmployeeRoleRepository.class));
        User locked = user(14L, "locked@example.com", lookup(1L, "ADMIN", "Administrator"), null);
        locked.setStatus("LOCKED");
        locked.setSessionVersion(4L);
        when(userRepository.findForSecurityUpdate(14L)).thenReturn(java.util.Optional.of(locked));
        when(userRepository.save(locked)).thenReturn(locked);

        UserManagementUserResponse result = service.updateStatus(14L, "active");

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(locked.getSessionVersion()).isEqualTo(5L);
        verify(userRepository).save(locked);
    }

    @Test
    void rejectsTransitionNotInAllowedList() {
        UserRepository userRepository = mock(UserRepository.class);
        UserManagementService service = new UserManagementServiceImpl(
                userRepository, mock(EmployeeAssignmentRepository.class), mock(SowRepository.class),
                mock(LookupValueRepository.class), mock(EmployeeRoleRepository.class));
        User active = user(12L, "active@example.com", lookup(1L, "ADMIN", "Administrator"), null);
        when(userRepository.findForSecurityUpdate(12L)).thenReturn(java.util.Optional.of(active));

        assertThatThrownBy(() -> service.updateStatus(12L, "ACTIVE"))
                .hasMessage("User status transition is not allowed: ACTIVE -> ACTIVE");
    }

    @Test
    void changesUserAndEmployeeRoleAndInvalidatesSessions() {
        UserRepository userRepository = mock(UserRepository.class);
        EmployeeAssignmentRepository assignmentRepository = mock(EmployeeAssignmentRepository.class);
        LookupValueRepository lookupRepository = mock(LookupValueRepository.class);
        EmployeeRoleRepository employeeRoleRepository = mock(EmployeeRoleRepository.class);
        UserManagementService service = new UserManagementServiceImpl(userRepository,
                assignmentRepository, mock(SowRepository.class), lookupRepository, employeeRoleRepository);

        Employee employee = new Employee();
        employee.setId(2L);
        employee.setFirstName("Charan");
        employee.setEmail("charan@gmail.com");
        User user = user(12L, "charan@gmail.com", lookup(30L, "EMPLOYEE", "Employee"), employee);
        user.setSessionVersion(2L);
        LookupValue manager = systemRole(31L, "MANAGER", "Manager");
        com.rit.performance.entity.EmployeeRole current = new com.rit.performance.entity.EmployeeRole();
        current.setEmployeeId(2L);
        current.setRoleId(30L);
        current.setEffectiveFrom(LocalDate.now().minusYears(1));
        current.setIsCurrent(true);

        when(userRepository.findForSecurityUpdate(12L)).thenReturn(java.util.Optional.of(user));
        when(lookupRepository.findById(31L)).thenReturn(java.util.Optional.of(manager));
        when(userRepository.save(user)).thenReturn(user);
        when(employeeRoleRepository.findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(2L))
                .thenReturn(java.util.Optional.of(current));
        when(assignmentRepository.findCurrentForEmployees(eq(List.of(2L)), any(LocalDate.class)))
                .thenReturn(List.of());

        UserManagementUserResponse result = service.updateRole(12L, 31L);

        assertThat(result.getRoleId()).isEqualTo(31L);
        assertThat(result.getRoleCode()).isEqualTo("MANAGER");
        assertThat(user.getSessionVersion()).isEqualTo(3L);
        assertThat(current.getIsCurrent()).isFalse();
        verify(employeeRoleRepository, org.mockito.Mockito.times(2)).save(any());
    }

    private static User user(Long id, String username, LookupValue role, Employee employee) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setEmployee(employee);
        user.setStatus("ACTIVE");
        return user;
    }

    private static LookupValue lookup(Long id, String code, String name) {
        LookupValue lookup = new LookupValue();
        lookup.setId(id);
        lookup.setCode(code);
        lookup.setName(name);
        return lookup;
    }

    private static LookupValue systemRole(Long id, String code, String name) {
        LookupType type = new LookupType();
        type.setCode("SYSTEM_ROLE");
        type.setActive(true);
        LookupValue role = lookup(id, code, name);
        role.setLookupType(type);
        role.setActive(true);
        return role;
    }
}
