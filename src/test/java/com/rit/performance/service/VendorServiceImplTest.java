package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.VendorRequest;
import com.rit.performance.dto.VendorDocumentRequest;
import com.rit.performance.dto.VendorPaymentDetailsRequest;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.entity.Document;
import com.rit.performance.repository.DocumentRepository;
import com.rit.performance.entity.Vendor;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VendorServiceImplTest {

    @Test
    void createNormalizesValuesAndDefaultsStatusToActive() {
        VendorRepository repository = mock(VendorRepository.class);
        VendorServiceImpl service = service(repository);
        VendorRequest request = request(" acme ", " Acme Corporation ");
        request.setContactEmail(" SALES@ACME.COM ");
        request.setCurrency(" USD ");
        when(repository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            vendor.setId(7L);
            return vendor;
        });

        var response = service.create(request);

        assertEquals(7L, response.getId());
        assertEquals("ACME", response.getVendorCode());
        assertEquals("Acme Corporation", response.getCompanyName());
        assertEquals("sales@acme.com", response.getContactEmail());
        assertEquals("USD", response.getCurrency());
        assertEquals("ACTIVE", response.getStatus());
        verify(repository).existsByVendorCodeIgnoreCase("ACME");
    }

    @Test
    void createRejectsDuplicateCodeIgnoringCase() {
        VendorRepository repository = mock(VendorRepository.class);
        VendorServiceImpl service = service(repository);
        when(repository.existsByVendorCodeIgnoreCase("ACME")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(request("acme", "Acme Corporation")));
        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownVendor() {
        VendorRepository repository = mock(VendorRepository.class);
        VendorServiceImpl service = service(repository);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, request("ACME", "Acme Corporation")));
    }

    @Test
    void getAllIncludesInactiveVendors() {
        VendorRepository repository = mock(VendorRepository.class);
        VendorServiceImpl service = service(repository);
        Vendor active = Vendor.builder().id(1L).vendorCode("A").companyName("Alpha")
                .status("ACTIVE").build();
        Vendor inactive = Vendor.builder().id(2L).vendorCode("B").companyName("Beta")
                .status("INACTIVE").build();
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(active, inactive));

        var result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("INACTIVE", result.get(1).getStatus());
    }

    @Test
    void createAssociatesExistingDocuments() {
        VendorRepository repository = mock(VendorRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, documentRepository, mock(BankAccountService.class));
        VendorRequest request = request("ACME", "Acme Corporation");
        request.setDocumentList(List.of(
                VendorDocumentRequest.builder().id(11L).build(),
                VendorDocumentRequest.builder().id(12L).build()));
        Document first = Document.builder().id(11L).documentName("W-9").build();
        Document second = Document.builder().id(12L).documentName("Contract").build();
        when(documentRepository.findAllById(any())).thenReturn(List.of(first, second));
        when(repository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            vendor.setId(7L);
            return vendor;
        });

        var response = service.create(request);

        assertEquals(List.of(11L, 12L), response.getDocumentList().stream()
                .map(document -> document.getId())
                .toList());
    }

    @Test
    void createRejectsDuplicateDocumentIds() {
        VendorRepository repository = mock(VendorRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, documentRepository, mock(BankAccountService.class));
        VendorRequest request = request("ACME", "Acme Corporation");
        request.setDocumentList(List.of(
                VendorDocumentRequest.builder().id(11L).build(),
                VendorDocumentRequest.builder().id(11L).build()));

        assertThrows(InvalidOperationException.class, () -> service.create(request));
        verify(documentRepository, never()).findAllById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingDocuments() {
        VendorRepository repository = mock(VendorRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, documentRepository, mock(BankAccountService.class));
        VendorRequest request = request("ACME", "Acme Corporation");
        request.setDocumentList(List.of(VendorDocumentRequest.builder().id(99L).build()));
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> service.create(request));
        assertTrue(exception.getMessage().contains("99"));
        verify(repository, never()).save(any());
    }

    @Test
    void createSavesNestedVendorPaymentDetails() {
        VendorRepository repository = mock(VendorRepository.class);
        BankAccountService bankAccountService = mock(BankAccountService.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, mock(DocumentRepository.class), bankAccountService);
        VendorRequest request = request("TEST", "TEST LLC");
        request.setPaymentDetails(VendorPaymentDetailsRequest.builder()
                .bankCountry("US")
                .currency("USD")
                .paymentMethod("ACH")
                .accountHolderName("TEST LLC")
                .bankName("Test Bank")
                .accountNumber("12345678903432")
                .routingNumber("0210004234")
                .remittanceEmail("payments@test.com")
                .build());
        when(repository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            vendor.setId(7L);
            return vendor;
        });
        when(bankAccountService.create(any())).thenReturn(BankAccountResponse.builder()
                .id(21L)
                .ownerType(BankAccountOwnerType.VENDOR)
                .ownerId(7L)
                .accountNumberLast4("3432")
                .routingNumberLast4("4234")
                .build());

        var response = service.create(request);

        ArgumentCaptor<BankAccountRequest> captor = ArgumentCaptor.forClass(
                BankAccountRequest.class);
        verify(bankAccountService).create(captor.capture());
        assertEquals(BankAccountOwnerType.VENDOR, captor.getValue().getOwnerType());
        assertEquals(7L, captor.getValue().getOwnerId());
        assertEquals("12345678903432", captor.getValue().getAccountNumberEncrypted());
        assertEquals("3432", captor.getValue().getAccountNumberLast4());
        assertEquals("0210004234", captor.getValue().getRoutingNumberEncrypted());
        assertEquals(21L, response.getPaymentDetails().getId());
        assertEquals("3432", response.getPaymentDetails().getAccountNumberLast4());
        assertEquals("4234", response.getPaymentDetails().getRoutingNumberLast4());
    }

    @Test
    void updateUsesExistingVendorPaymentAccountForIndiaDetails() {
        VendorRepository repository = mock(VendorRepository.class);
        BankAccountService bankAccountService = mock(BankAccountService.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, mock(DocumentRepository.class), bankAccountService);
        Vendor vendor = Vendor.builder().id(7L).vendorCode("TEST")
                .companyName("TEST LLC").status("ACTIVE").build();
        VendorRequest request = request("TEST", "TEST LLC");
        request.setPaymentDetails(VendorPaymentDetailsRequest.builder()
                .bankCountry("IN")
                .currency("INR")
                .ifscCode("hdfc0001234")
                .branch("Mumbai")
                .build());
        when(repository.findById(7L)).thenReturn(Optional.of(vendor));
        when(repository.save(vendor)).thenReturn(vendor);
        when(bankAccountService.getAll(BankAccountOwnerType.VENDOR, 7L))
                .thenReturn(List.of(BankAccountResponse.builder().id(21L).isPrimary(true).build()));
        when(bankAccountService.update(eq(21L), any())).thenReturn(
                BankAccountResponse.builder().id(21L).ifscCode("HDFC0001234")
                        .branchName("Mumbai").build());

        var response = service.update(7L, request);

        ArgumentCaptor<BankAccountRequest> captor = ArgumentCaptor.forClass(
                BankAccountRequest.class);
        verify(bankAccountService).update(eq(21L), captor.capture());
        assertEquals("hdfc0001234", captor.getValue().getIfscCode());
        assertEquals("Mumbai", captor.getValue().getBranchName());
        assertEquals("HDFC0001234", response.getPaymentDetails().getIfscCode());
    }

    @Test
    void updateDoesNotSaveEmptyPaymentDetails() {
        VendorRepository repository = mock(VendorRepository.class);
        BankAccountService bankAccountService = mock(BankAccountService.class);
        VendorServiceImpl service = new VendorServiceImpl(
                repository, mock(DocumentRepository.class), bankAccountService);
        Vendor vendor = Vendor.builder().id(7L).vendorCode("TEST")
                .companyName("TEST LLC").status("ACTIVE").build();
        VendorRequest request = request("TEST", "TEST LLC");
        request.setPaymentDetails(new VendorPaymentDetailsRequest());
        when(repository.findById(7L)).thenReturn(Optional.of(vendor));
        when(repository.save(vendor)).thenReturn(vendor);

        service.update(7L, request);

        verify(bankAccountService, never()).create(any());
        verify(bankAccountService, never()).update(any(), any());
    }

    private VendorRequest request(String code, String companyName) {
        return VendorRequest.builder()
                .vendorCode(code)
                .companyName(companyName)
                .build();
    }

    private VendorServiceImpl service(VendorRepository repository) {
        return new VendorServiceImpl(
                repository,
                mock(DocumentRepository.class),
                mock(BankAccountService.class));
    }
}
