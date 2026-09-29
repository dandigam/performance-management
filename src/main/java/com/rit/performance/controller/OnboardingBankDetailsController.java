package com.rit.performance.controller;

import com.rit.performance.dto.OnboardingBankDetailsRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingBankDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding/me/bank-details")
@RequiredArgsConstructor
public class OnboardingBankDetailsController {
    private final OnboardingBankDetailsService service;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody OnboardingBankDetailsRequest request) {
        return service.update(request);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "INVALID_BANK_DETAILS",
                "message", "Provide bankCountry (India or USA), currency, accountHolderName, bankName, accountNumber and bankCode; USA bankCode must contain 9 digits."));
    }
}
