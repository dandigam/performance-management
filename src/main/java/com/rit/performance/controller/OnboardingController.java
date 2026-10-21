package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingCreateRequest;
import com.rit.performance.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/hr/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<String> create(@RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody OnboardingCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                .body(service.create(principal.getName(), key, request));
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<java.util.Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(java.util.Map.of(
                "code", "INVALID_ONBOARDING_REQUEST", "message", "Invalid or missing onboarding fields."));
    }
}
