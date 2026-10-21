package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingEmploymentHistoryRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingEmploymentHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding/me/employment-history")
@RequiredArgsConstructor
public class OnboardingEmploymentHistoryController {
    private final OnboardingEmploymentHistoryService service;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody OnboardingEmploymentHistoryRequest request) {
        return service.update(request);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "INVALID_EMPLOYMENT_HISTORY",
                "message", "Provide companyName, position, location and fromDate; endDate must be on or after fromDate, or null for ongoing employment."));
    }
}
