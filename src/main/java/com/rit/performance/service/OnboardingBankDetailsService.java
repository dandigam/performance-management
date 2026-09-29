package com.rit.performance.service;

import com.rit.performance.dto.OnboardingBankDetailsRequest;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.BankAccount;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.BankAccountRepository;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingBankDetailsService {
    private final CurrentEmployeeService currentEmployee;
    private final BankAccountRepository banks;
    private final EmployeeOnboardingRepository onboardings;
    private final OnboardingSelfService selfService;

    @Transactional
    public OnboardingResponse update(OnboardingBankDetailsRequest request) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Bank details cannot be changed at this onboarding stage.");
        var account = banks.findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                BankAccountOwnerType.EMPLOYEE, employee.getId()).orElseGet(() -> BankAccount.builder()
                        .ownerType(BankAccountOwnerType.EMPLOYEE).ownerId(employee.getId())
                        .isPrimary(true).active(true).build());
        boolean india = "India".equalsIgnoreCase(request.bankCountry());
        account.setBankCountry(india ? "INDIA" : "USA");
        account.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        account.setAccountHolderName(request.accountHolderName().trim());
        account.setBankName(request.bankName().trim());
        account.setPaymentMethod("BANK_TRANSFER");
        String number = request.accountNumber().trim();
        if (number.length() < 4)
            throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_BANK_DETAILS",
                    "Account number must contain at least four characters.");
        // Follow the existing employee bank-details storage convention.
        account.setAccountNumberEncrypted(number);
        account.setAccountNumberLast4(number.substring(number.length() - 4));
        account.setIfscCode(india ? request.bankCode().trim().toUpperCase(Locale.ROOT) : null);
        account.setRoutingNumberEncrypted(india ? null : request.bankCode().trim());
        banks.saveAndFlush(account);
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return selfService.getMine();
    }
}
