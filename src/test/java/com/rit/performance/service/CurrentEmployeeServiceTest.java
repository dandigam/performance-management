package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentEmployeeServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentEmployeeService service = new CurrentEmployeeService(users);

    @AfterEach void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test void resolvesEmployeeOnlyFromAuthenticatedAccount() {
        AuthenticatedUser principal = new AuthenticatedUser(9L, "employee", "", true, 0, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        Employee employee = new Employee(); employee.setId(3L);
        User user = new User(); user.setId(9L); user.setEmployee(employee);
        when(users.findById(9L)).thenReturn(Optional.of(user));
        assertEquals(3L, service.currentEmployee().getId());
        verify(users).findById(9L);
        user.setEmployee(null);
        assertThrows(AuthenticationException.class, service::currentEmployee);
    }

    @Test void rejectsMissingAuthentication() {
        assertThrows(AuthenticationException.class, service::currentEmployee);
        verifyNoInteractions(users);
    }
}
