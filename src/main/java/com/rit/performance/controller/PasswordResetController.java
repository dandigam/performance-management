package com.rit.performance.controller;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.ApiMessageResponse;
import com.rit.performance.exception.InvalidOperationException;
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

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        if (!limiter.allow("reset-ip:" + http.getRemoteAddr(), 30, Duration.ofMinutes(15)))
            return ResponseEntity.status(429).header("Retry-After", "900")
                    .body(ApiMessageResponse.warning("RATE_LIMIT_EXCEEDED",
                            "Too many requests. Please try again later."));
        service.reset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiMessageResponse.success("Password reset successfully."));
    }

    @ExceptionHandler(com.rit.performance.exception.PasswordResetRateLimitException.class)
    public ResponseEntity<ApiMessageResponse> rateLimited(com.rit.performance.exception.PasswordResetRateLimitException e) {
        return ResponseEntity.status(429).header("Retry-After", "3600")
                .body(ApiMessageResponse.warning("RATE_LIMIT_EXCEEDED", e.getMessage()));
    }
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiMessageResponse> invalid(InvalidOperationException e) {
        return ResponseEntity.badRequest().body(ApiMessageResponse.warning(e.getCode(), e.getMessage()));
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
