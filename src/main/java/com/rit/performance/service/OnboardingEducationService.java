package com.rit.performance.service;

import com.rit.performance.dto.OnboardingEducationRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.EmployeeEducation;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeEducationRepository;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingEducationService {
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeEducationRepository educations;
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService selfService;

    @Transactional
    public OnboardingResponse update(OnboardingEducationRequest request) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Education details cannot be changed at this onboarding stage.");
        // Replace only this employee's list under the onboarding lock, in one transaction.
        educations.deleteByEmployeeId(employee.getId());
        educations.saveAllAndFlush(request.educationDetails().stream().map(detail ->
                EmployeeEducation.builder().employee(employee)
                        .educationType(detail.degree().trim())
                        .collegeUniversity(detail.institution().trim())
                        .passingYear(detail.passingYear()).percentage(detail.percentage()).build()).toList());
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return selfService.getMine();
    }
}
