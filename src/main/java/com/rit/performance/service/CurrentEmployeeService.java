package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentEmployeeService {
    private final UserRepository users;

    public Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal))
            throw new AuthenticationException("Employee authentication is required.");
        User user = users.findById(principal.id())
                .orElseThrow(() -> new AuthenticationException("User account is not available."));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || user.getEmployee() == null)
            throw new AuthenticationException("An active employee account is required.");
        return user.getEmployee();
    }
}
