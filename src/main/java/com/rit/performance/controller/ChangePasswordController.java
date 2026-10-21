package com.rit.performance.controller;

import com.rit.performance.dto.request.ChangePasswordRequest;
import com.rit.performance.dto.ApiMessageResponse;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.service.ChangePasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class ChangePasswordController {
    private final ChangePasswordService service;

    @PutMapping("/change-password")
    public ResponseEntity<ApiMessageResponse> change(@AuthenticationPrincipal UserDetails user,
                                      @RequestBody ChangePasswordRequest request) {
        service.change(user == null ? null : user.getUsername(), request);
        return ResponseEntity.ok(ApiMessageResponse.success("Password changed successfully."));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiMessageResponse> invalid(InvalidOperationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiMessageResponse.warning(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessageResponse> malformed(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiMessageResponse.warning("INVALID_REQUEST", "Invalid request body."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiMessageResponse> unauthorized(AuthenticationException exception) {
        return ResponseEntity.status(401)
                .body(ApiMessageResponse.warning("AUTHENTICATION_FAILED", exception.getMessage()));
    }
}
