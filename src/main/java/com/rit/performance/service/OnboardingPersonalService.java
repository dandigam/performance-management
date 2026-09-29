package com.rit.performance.service;

import com.rit.performance.dto.OnboardingPersonalRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingPersonalService {
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeRepository employees;
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService selfService;

    @Transactional
    public OnboardingResponse update(OnboardingPersonalRequest request) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Personal details cannot be changed at this onboarding stage.");
        if (employees.existsByPhoneNumberAndIdNot(request.phoneNumber(), employee.getId()))
            throw new ApplicationException(HttpStatus.CONFLICT, "EMPLOYEE_PHONE_EXISTS",
                    "An employee with this phone number already exists.");
        employee.setPhoneNumber(request.phoneNumber());
        employee.setGender(request.gender());
        employee.setDateOfBirth(request.dateOfBirth());
        employees.saveAndFlush(employee);
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        // A section update must advance the onboarding version even after the first save.
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return selfService.getMine();
    }
}
