package com.rit.performance.controller;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.service.OnboardingDocumentsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/onboarding/me/documents")
@RequiredArgsConstructor
public class OnboardingDocumentsController {
    private final OnboardingDocumentsService service;
    public record Request(@NotNull List<@NotNull @Positive Long> documentIds) {}
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public OnboardingResponse update(@Valid @RequestBody Request request) {
        return service.update(request.documentIds());
    }
}
