package com.rit.performance.service;

import com.rit.performance.dto.LoginRequest;
import com.rit.performance.dto.LoginResponse;

public interface AuthenticationService {
    LoginResponse login(LoginRequest request);
}
