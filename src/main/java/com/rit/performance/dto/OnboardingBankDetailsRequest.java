package com.rit.performance.dto;

import jakarta.validation.constraints.*;

public record OnboardingBankDetailsRequest(
        @NotBlank @Pattern(regexp = "(?i)(India|USA|US|United States)") String bankCountry,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotBlank @Size(max = 200) String accountHolderName,
        @NotBlank @Size(max = 200) String bankName,
        @NotBlank @Size(min = 4, max = 100) String accountNumber,
        @NotBlank @Size(max = 20) String bankCode) {
    @AssertTrue(message = "USA bankCode must contain exactly 9 digits")
    public boolean isBankCodeValid() {
        return bankCountry == null || bankCode == null || bankCountry.equalsIgnoreCase("India")
                || bankCode.trim().matches("[0-9]{9}");
    }
}
