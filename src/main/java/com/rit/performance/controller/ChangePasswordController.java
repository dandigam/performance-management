package com.rit.performance.controller;

import com.rit.performance.dto.request.ChangePasswordRequest;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.service.ChangePasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class ChangePasswordController {
    private final ChangePasswordService service;

    @PutMapping("/change-password")
    public ResponseEntity<Void> change(@AuthenticationPrincipal UserDetails user,
                                      @RequestBody ChangePasswordRequest request) {
        service.change(user == null ? null : user.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidOperationException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformed(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> unauthorized(AuthenticationException exception) {
        return ResponseEntity.status(401).body(Map.of("message", exception.getMessage()));
    }
}
