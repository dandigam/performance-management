package com.rit.performance.service;

import com.rit.performance.dto.OnboardingEmploymentHistoryRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.EmployeeExperience;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeExperienceRepository;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingEmploymentHistoryService {
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeExperienceRepository experiences;
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService selfService;

    @Transactional
    public OnboardingResponse update(OnboardingEmploymentHistoryRequest request) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Employment history cannot be changed at this onboarding stage.");
        // Replace only this employee's list under the onboarding lock, in one transaction.
        experiences.deleteByEmployeeId(employee.getId());
        experiences.saveAllAndFlush(request.experienceDetails().stream().map(detail ->
                EmployeeExperience.builder().employee(employee)
                        .companyName(detail.companyName().trim())
                        .position(detail.position().trim()).location(detail.location().trim())
                        .fromDate(detail.fromDate()).endDate(detail.endDate()).build()).toList());
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return selfService.getMine();
    }
}
