package com.rit.performance.dto;

import com.rit.performance.entity.BankAccountOwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountResponse {
    private Long id;
    private BankAccountOwnerType ownerType;
    private Long ownerId;
    private String bankCountry;
    private String currency;
    private String paymentMethod;
    private String accountHolderName;
    private String bankName;
    private String accountType;
    private String accountNumberLast4;
    private String routingNumberLast4;
    private String ifscCode;
    private String branchName;
    private String remittanceEmail;
    private boolean isPrimary;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
