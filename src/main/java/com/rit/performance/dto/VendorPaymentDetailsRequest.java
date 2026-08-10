package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorPaymentDetailsRequest {
    @Size(max = 100)
    private String bankCountry;

    @Size(max = 10)
    private String currency;

    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 50)
    private String accountType;

    @Size(max = 200)
    private String accountHolderName;

    @Size(max = 200)
    private String bankName;

    @Size(max = 255)
    private String accountNumber;

    @Size(max = 255)
    private String routingNumber;

    @Size(max = 20)
    private String ifscCode;

    @JsonAlias("branchName")
    @Size(max = 200)
    private String branch;

    @Email(message = "remittanceEmail must be valid")
    @Size(max = 150)
    private String remittanceEmail;

    public boolean hasValues() {
        return hasText(bankCountry)
                || hasText(currency)
                || hasText(paymentMethod)
                || hasText(accountType)
                || hasText(accountHolderName)
                || hasText(bankName)
                || hasText(accountNumber)
                || hasText(routingNumber)
                || hasText(ifscCode)
                || hasText(branch)
                || hasText(remittanceEmail);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
