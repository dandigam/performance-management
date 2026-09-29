package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingPersonalRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingPersonalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding/me/personal")
@RequiredArgsConstructor
public class OnboardingPersonalController {
    private final OnboardingPersonalService service;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody OnboardingPersonalRequest request) {
        return service.update(request);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "INVALID_PERSONAL_DETAILS",
                "message", "Provide a valid phone number, gender, and past date of birth."));
    }
}
