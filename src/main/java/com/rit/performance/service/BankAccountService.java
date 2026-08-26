package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.BankAccountSensitiveDetailsResponse;
import com.rit.performance.entity.BankAccountOwnerType;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse create(BankAccountRequest request);
    BankAccountResponse update(Long id, BankAccountRequest request);
    BankAccountResponse getById(Long id);
    BankAccountSensitiveDetailsResponse getSensitiveDetails(Long id);
    List<BankAccountResponse> getAll(BankAccountOwnerType ownerType, Long ownerId);
}
