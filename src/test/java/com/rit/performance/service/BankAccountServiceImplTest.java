package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.entity.BankAccount;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankAccountServiceImplTest {

    @Test
    void createStoresOwnerWithoutAParentEntityRelationship() {
        BankAccountRepository repository = mock(BankAccountRepository.class);
        BankAccountServiceImpl service = new BankAccountServiceImpl(repository);
        BankAccountRequest request = request();
        request.setCurrency(" usd ");
        request.setRemittanceEmail(" PAYMENTS@EXAMPLE.COM ");
        request.setAccountNumberEncrypted("ciphertext");
        request.setRoutingNumberEncrypted("0210004234");
        when(repository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(7L);
            return account;
        });

        var response = service.create(request);

        assertEquals(7L, response.getId());
        assertEquals(BankAccountOwnerType.VENDOR, response.getOwnerType());
        assertEquals(12L, response.getOwnerId());
        assertEquals("USD", response.getCurrency());
        assertEquals("payments@example.com", response.getRemittanceEmail());
        assertEquals("4234", response.getRoutingNumberLast4());
    }

    @Test
    void makingAnAccountPrimaryDemotesTheExistingPrimary() {
        BankAccountRepository repository = mock(BankAccountRepository.class);
        BankAccountServiceImpl service = new BankAccountServiceImpl(repository);
        BankAccount existing = BankAccount.builder().id(3L)
                .ownerType(BankAccountOwnerType.VENDOR).ownerId(12L)
                .isPrimary(true).active(true).build();
        BankAccountRequest request = request();
        request.setIsPrimary(true);
        when(repository.findByOwnerTypeAndOwnerIdOrderByIsPrimaryDescIdAsc(
                BankAccountOwnerType.VENDOR, 12L)).thenReturn(List.of(existing));
        when(repository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        assertFalse(existing.isPrimary());
    }

    @Test
    void updatePreservesEncryptedValuesWhenTheyAreOmitted() {
        BankAccountRepository repository = mock(BankAccountRepository.class);
        BankAccountServiceImpl service = new BankAccountServiceImpl(repository);
        BankAccount existing = BankAccount.builder().id(5L)
                .ownerType(BankAccountOwnerType.VENDOR).ownerId(12L)
                .accountNumberEncrypted("account-ciphertext")
                .accountNumberLast4("1234")
                .routingNumberEncrypted("routing-ciphertext")
                .active(true).build();
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(5L, request());

        assertEquals("account-ciphertext", existing.getAccountNumberEncrypted());
        assertEquals("1234", existing.getAccountNumberLast4());
        assertEquals("routing-ciphertext", existing.getRoutingNumberEncrypted());
        verify(repository).save(existing);
    }

    @Test
    void listRequiresBothOwnerFilters() {
        BankAccountServiceImpl service = new BankAccountServiceImpl(mock(BankAccountRepository.class));

        assertThrows(InvalidOperationException.class,
                () -> service.getAll(BankAccountOwnerType.EMPLOYEE, null));
    }

    @Test
    void sensitiveDetailsReturnsFullStoredAccountAndRoutingValues() {
        BankAccountRepository repository = mock(BankAccountRepository.class);
        BankAccountServiceImpl service = new BankAccountServiceImpl(repository);
        BankAccount account = BankAccount.builder()
                .id(7L)
                .ownerType(BankAccountOwnerType.VENDOR)
                .ownerId(12L)
                .accountNumberEncrypted("12345678903432")
                .routingNumberEncrypted("0210004234")
                .build();
        when(repository.findById(7L)).thenReturn(Optional.of(account));

        var response = service.getSensitiveDetails(7L);

        assertEquals("12345678903432", response.getAccountNumber());
        assertEquals("0210004234", response.getRoutingNumber());
        assertEquals(BankAccountOwnerType.VENDOR, response.getOwnerType());
        assertEquals(12L, response.getOwnerId());
    }

    private BankAccountRequest request() {
        return BankAccountRequest.builder()
                .ownerType(BankAccountOwnerType.VENDOR)
                .ownerId(12L)
                .active(true)
                .build();
    }
}
