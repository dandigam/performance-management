package com.rit.performance.service;

import com.rit.performance.dto.LoginRequest;
import com.rit.performance.dto.LoginResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.getUserId().trim())
                .orElseThrow(() -> new AuthenticationException("Invalid user ID or password"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationException("User account is inactive");
        }
        if (!passwordMatches(request.getPwd(), user.getPassword())) {
            throw new AuthenticationException("Invalid user ID or password");
        }

        Employee employee = user.getEmployee();
        String employeeName = employee == null ? null
                : (employee.getFirstName() + " "
                    + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return LoginResponse.builder()
                .userId(user.getId()).username(user.getUsername())
                .roleId(user.getRole().getId()).roleName(user.getRole().getName())
                .employeeId(employee == null ? null : employee.getId())
                .employeeName(employeeName).status(user.getStatus()).build();
    }

    private boolean passwordMatches(String suppliedPassword, String storedPassword) {
        if (storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(suppliedPassword, storedPassword);
        }
        return MessageDigest.isEqual(suppliedPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
