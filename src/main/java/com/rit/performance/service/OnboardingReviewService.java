package com.rit.performance.service;

import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class OnboardingReviewService {
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService summaries;

    public Page<OnboardingResponse> queue(String status, int page, int size) {
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        if (page < 0 || size < 1 || size > 100 || (filter != null && !java.util.Set.of(
                "INVITED", "IN_PROGRESS", "SUBMITTED", "CHANGES_REQUESTED", "APPROVED", "REJECTED").contains(filter)))
            throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ONBOARDING_QUERY",
                    "Use a valid onboarding status, page >= 0 and size from 1 to 100.");
        return onboardings.findQueue(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(onboarding -> summaries.responseFor(onboarding.getEmployee(), onboarding, false));
    }

    public OnboardingResponse review(Long employeeId) {
        var onboarding = onboardings.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for this employee."));
        return summaries.responseFor(onboarding.getEmployee(), onboarding);
    }
}
