package com.rit.performance.service;

import com.rit.performance.dto.LoginRequest;
import com.rit.performance.dto.LoginResponse;

public interface AuthenticationService {
    AuthenticationResult login(LoginRequest request);
    AuthenticationResult refresh(String refreshToken);
    void logout(String refreshToken);
    LoginResponse currentUser(String username);

    record AuthenticationResult(LoginResponse response, String refreshToken) {
    }
}
