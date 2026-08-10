package com.rit.performance.service;

import com.rit.performance.dto.LoginRequest;
import com.rit.performance.dto.LoginResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        String username = request.getUserId().trim();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPwd()));
            username = authentication.getName();
        } catch (org.springframework.security.core.AuthenticationException exception) {
            throw new AuthenticationException("Invalid user ID or password");
        }

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AuthenticationException("Invalid user ID or password"));
        if (passwordEncoder.upgradeEncoding(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPwd()));
            userRepository.save(user);
        }
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return new AuthenticationResult(toResponse(user), refreshToken.value());
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(String refreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return new AuthenticationResult(toResponse(rotated.user()), rotated.value());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse currentUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AuthenticationException("User account is not available"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationException("User account is inactive");
        }
        return toUserResponse(user, null);
    }

    private LoginResponse toResponse(User user) {
        JwtService.AccessToken accessToken = jwtService.createAccessToken(user);
        return toUserResponse(user, accessToken);
    }

    private LoginResponse toUserResponse(User user, JwtService.AccessToken accessToken) {
        Employee employee = user.getEmployee();
        String employeeName = employee == null ? null
                : (employee.getFirstName() + " "
                    + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return LoginResponse.builder()
                .token(accessToken == null ? null : accessToken.value())
                .expiresAt(accessToken == null ? null : accessToken.expiresAt())
                .userId(user.getId()).username(user.getUsername())
                .roleId(user.getRole().getId()).roleName(user.getRole().getName())
                .employeeId(employee == null ? null : employee.getId())
                .employeeName(employeeName).status(user.getStatus()).build();
    }
}
