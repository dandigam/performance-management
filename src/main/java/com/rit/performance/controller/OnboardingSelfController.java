package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingSelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding/me")
@RequiredArgsConstructor
public class OnboardingSelfController {
    private final OnboardingSelfService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse getMine() {
        return service.getMine();
    }

    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse submit() {
        return service.submit();
    }
}
