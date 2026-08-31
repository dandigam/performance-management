package com.rit.performance.mapper;

import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.entity.BankAccount;

public final class BankAccountMapper {
    private BankAccountMapper() {
    }

    public static BankAccountResponse toResponse(BankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .ownerType(account.getOwnerType())
                .ownerId(account.getOwnerId())
                .bankCountry(account.getBankCountry())
                .currency(account.getCurrency())
                .paymentMethod(account.getPaymentMethod())
                .accountHolderName(account.getAccountHolderName())
                .bankName(account.getBankName())
                .accountType(account.getAccountType())
                .accountNumberLast4(account.getAccountNumberLast4())
                .routingNumberLast4(lastFour(account.getRoutingNumberEncrypted()))
                .ifscCode(account.getIfscCode())
                .branchName(account.getBranchName())
                .remittanceEmail(account.getRemittanceEmail())
                .isPrimary(account.isPrimary())
                .active(account.isActive())
                .createdAt(account.getCreatedOn())
                .updatedAt(account.getUpdatedOn())
                .build();
    }

    private static String lastFour(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= 4
                ? trimmed : trimmed.substring(trimmed.length() - 4);
    }
}
