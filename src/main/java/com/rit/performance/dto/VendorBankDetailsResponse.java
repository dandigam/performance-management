package com.rit.performance.dto;

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
public class VendorBankDetailsResponse {
    private Long id;
    private String bankCountry;
    private String currency;
    private String paymentMethod;
    private String accountType;
    private String accountHolderName;
    private String bankName;
    private String accountNumberLast4;
    private String routingNumberLast4;
    private String ifscCode;
    private String branch;
    private String remittanceEmail;
}
