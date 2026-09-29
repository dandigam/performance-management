package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingEducationRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingEducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding/me/education")
@RequiredArgsConstructor
public class OnboardingEducationController {
    private final OnboardingEducationService service;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody OnboardingEducationRequest request) {
        return service.update(request);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "INVALID_EDUCATION_DETAILS",
                "message", "Provide education details with degree, institution, passing year and percentage from 0 to 100."));
    }
}
