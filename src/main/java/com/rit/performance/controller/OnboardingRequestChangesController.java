package com.rit.performance.controller;

import com.rit.performance.dto.*;
import com.rit.performance.service.OnboardingRequestChangesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/hr/onboarding")
@RequiredArgsConstructor
public class OnboardingRequestChangesController {
    private final OnboardingRequestChangesService service;

    @PostMapping("/{employeeId}/request-changes")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public OnboardingResponse requestChanges(@PathVariable Long employeeId,
            @Valid @RequestBody OnboardingRequestChangesRequest request, Principal principal) {
        return service.requestChanges(employeeId, request, principal.getName());
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> invalidRequest(Exception exception) {
        return org.springframework.http.ResponseEntity.unprocessableEntity().body(java.util.Map.of(
                "code", "INVALID_REVIEW_REQUEST", "message", "Provide version and nonblank comments of at most 5000 characters."));
    }
}
