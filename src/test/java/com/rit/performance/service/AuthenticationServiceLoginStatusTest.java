package com.rit.performance.service;

import com.rit.performance.dto.LoginRequest;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AccountUnavailableException;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceLoginStatusTest {

    @Test
    void inactiveAccountReturnsAccountUnavailableWithoutCheckingPassword() {
        UserRepository users = mock(UserRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthenticationService service = service(users, authenticationManager);
        User user = user("INACTIVE");
        when(users.findForAuthentication("charan@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(AccountUnavailableException.class)
                .hasMessage("This account is unavailable. Please contact your administrator.");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void incorrectPasswordReturnsUnauthorizedMessage() {
        UserRepository users = mock(UserRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthenticationService service = service(users, authenticationManager);
        when(users.findForAuthentication("charan@gmail.com")).thenReturn(Optional.of(user("ACTIVE")));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("The email or password is incorrect.");
    }

    private static AuthenticationService service(UserRepository users,
            AuthenticationManager authenticationManager) {
        return new AuthenticationServiceImpl(users, mock(PasswordEncoder.class),
                authenticationManager, mock(JwtService.class), mock(RefreshTokenService.class));
    }

    private static LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setUserId("charan@gmail.com");
        request.setPwd("admin123");
        return request;
    }

    private static User user(String status) {
        User user = new User();
        user.setUsername("charan@gmail.com");
        user.setStatus(status);
        return user;
    }
}
