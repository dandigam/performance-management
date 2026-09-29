package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingAddressRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding/me/address")
@RequiredArgsConstructor
public class OnboardingAddressController {
    private final OnboardingAddressService service;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody OnboardingAddressRequest request) {
        return service.update(request);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "INVALID_ADDRESS_DETAILS",
                "message", "Provide valid address line 1, city, state, postal code and country."));
    }
}
