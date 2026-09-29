package com.rit.performance.service;

import com.rit.performance.dto.OnboardingAddressRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.EmployeeAddress;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeAddressRepository;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingAddressService {
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeAddressRepository addresses;
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService selfService;

    @Transactional
    public OnboardingResponse update(OnboardingAddressRequest request) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Address details cannot be changed at this onboarding stage.");
        // The onboarding lock serializes concurrent address creates for this employee.
        var address = addresses.findByEmployeeId(employee.getId()).orElseGet(EmployeeAddress::new);
        address.setEmployee(employee);
        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(request.addressLine2() == null || request.addressLine2().isBlank()
                ? null : request.addressLine2().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
        addresses.saveAndFlush(address);
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return selfService.getMine();
    }
}
