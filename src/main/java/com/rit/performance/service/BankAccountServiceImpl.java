package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.BankAccountSensitiveDetailsResponse;
import com.rit.performance.entity.BankAccount;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.BankAccountMapper;
import com.rit.performance.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankAccountServiceImpl implements BankAccountService {
    private final BankAccountRepository repository;

    @Override
    @Transactional
    public BankAccountResponse create(BankAccountRequest request) {
        BankAccount account = new BankAccount();
        apply(account, request, false);
        ensureOnlyPrimary(account, null);
        return BankAccountMapper.toResponse(repository.save(account));
    }

    @Override
    @Transactional
    public BankAccountResponse update(Long id, BankAccountRequest request) {
        BankAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + id));
        apply(account, request, true);
        ensureOnlyPrimary(account, id);
        return BankAccountMapper.toResponse(repository.save(account));
    }

    @Override
    public BankAccountResponse getById(Long id) {
        return repository.findById(id)
                .map(BankAccountMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + id));
    }

    @Override
    public BankAccountSensitiveDetailsResponse getSensitiveDetails(Long id) {
        BankAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bank account not found: " + id));
        return BankAccountSensitiveDetailsResponse.builder()
                .id(account.getId())
                .ownerType(account.getOwnerType())
                .ownerId(account.getOwnerId())
                .accountNumber(account.getAccountNumberEncrypted())
                .routingNumber(account.getRoutingNumberEncrypted())
                .build();
    }

    @Override
    public List<BankAccountResponse> getAll(BankAccountOwnerType ownerType, Long ownerId) {
        if ((ownerType == null) != (ownerId == null)) {
            throw new InvalidOperationException("ownerType and ownerId must be provided together");
        }
        List<BankAccount> accounts = ownerType == null
                ? repository.findAll(Sort.by("ownerType", "ownerId")
                        .and(Sort.by(Sort.Direction.DESC, "isPrimary"))
                        .and(Sort.by("id")))
                : repository.findByOwnerTypeAndOwnerIdOrderByIsPrimaryDescIdAsc(ownerType, ownerId);
        return accounts.stream().map(BankAccountMapper::toResponse).toList();
    }

    private void apply(BankAccount account, BankAccountRequest request, boolean updating) {
        account.setOwnerType(request.getOwnerType());
        account.setOwnerId(request.getOwnerId());
        account.setBankCountry(normalizeUpper(request.getBankCountry()));
        account.setCurrency(normalizeUpper(request.getCurrency()));
        account.setPaymentMethod(trimToNull(request.getPaymentMethod()));
        account.setAccountHolderName(trimToNull(request.getAccountHolderName()));
        account.setBankName(trimToNull(request.getBankName()));
        account.setAccountType(trimToNull(request.getAccountType()));
        if (!updating || request.getAccountNumberEncrypted() != null) {
            account.setAccountNumberEncrypted(trimToNull(request.getAccountNumberEncrypted()));
        }
        if (!updating || request.getAccountNumberLast4() != null) {
            account.setAccountNumberLast4(trimToNull(request.getAccountNumberLast4()));
        }
        if (!updating || request.getRoutingNumberEncrypted() != null) {
            account.setRoutingNumberEncrypted(trimToNull(request.getRoutingNumberEncrypted()));
        }
        account.setIfscCode(normalizeUpper(request.getIfscCode()));
        account.setBranchName(trimToNull(request.getBranchName()));
        account.setRemittanceEmail(normalizeEmail(request.getRemittanceEmail()));
        account.setPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        account.setActive(request.getActive() == null || request.getActive());
        if (account.isPrimary() && !account.isActive()) {
            throw new InvalidOperationException("an inactive bank account cannot be primary");
        }
    }

    private void ensureOnlyPrimary(BankAccount account, Long currentId) {
        if (!account.isPrimary()) return;
        repository.findByOwnerTypeAndOwnerIdOrderByIsPrimaryDescIdAsc(
                        account.getOwnerType(), account.getOwnerId()).stream()
                .filter(existing -> existing.isPrimary())
                .filter(existing -> currentId == null || !currentId.equals(existing.getId()))
                .forEach(existing -> existing.setPrimary(false));
    }

    private String normalizeUpper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
