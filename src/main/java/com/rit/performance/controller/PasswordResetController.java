package com.rit.performance.controller;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.ApiMessageResponse;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class PasswordResetController {
    private final PasswordResetService service;
    private final PasswordResetRateLimiter limiter;
    public static final String ACK = "If an account exists for this email, a reset link has been sent.";

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request,
                                                     HttpServletRequest http) {
        service.forgot(request.getEmail(), http.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiMessageResponse.success(ACK));
    }

    @PostMapping("/password-otp")
    public ResponseEntity<ApiMessageResponse> sendOtp(@Valid @RequestBody PasswordOtpRequest request,
                                                     HttpServletRequest http) {
        service.sendOtp(request.getToken(), http.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiMessageResponse.success("A verification code has been sent to your account email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        if (!limiter.allow("reset-ip:" + http.getRemoteAddr(), 30, Duration.ofMinutes(15))) {
            throw new ApplicationException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later.", "900");
        }
        service.reset(request.getToken(), request.getNewPassword(), request.getOtp());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiMessageResponse.success("Password reset successfully."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageResponse> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiMessageResponse.warning("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessageResponse> malformed(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiMessageResponse.warning("INVALID_REQUEST", "Invalid request body."));
    }
}
