package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.VendorRequest;
import com.rit.performance.dto.VendorResponse;
import com.rit.performance.dto.VendorDocumentRequest;
import com.rit.performance.dto.VendorBankDetailsRequest;
import com.rit.performance.dto.VendorBankDetailsResponse;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Vendor;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.VendorMapper;
import com.rit.performance.repository.DocumentRepository;
import com.rit.performance.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorServiceImpl implements VendorService {
    private final VendorRepository repository;
    private final DocumentRepository documentRepository;
    private final BankAccountService bankAccountService;

    @Override
    @Transactional
    public VendorResponse create(VendorRequest request) {
        validateTaxIdentifierAvailable(null, request.getTaxIdentifier());

        Vendor vendor = new Vendor();
        apply(vendor, request);
        vendor = repository.save(vendor);
        validateBankDetails(vendor, request.getBankDetails(), false);
        BankAccountResponse bankDetails = createBankDetailsIfPresent(
                vendor, request.getBankDetails());
        return toResponse(vendor, bankDetails);
    }

    @Override
    @Transactional
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor vendor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
        validateTaxIdentifierAvailable(id, request.getTaxIdentifier());

        apply(vendor, request);
        vendor = repository.save(vendor);
        validateBankDetails(vendor, request.getBankDetails(), true);
        BankAccountResponse bankDetails = synchronizeBankDetails(
                vendor, request.getBankDetails());
        return toResponse(vendor, bankDetails);
    }

    @Override
    public List<VendorResponse> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "companyName")).stream()
                .map(vendor -> toResponse(vendor, findBankDetails(vendor.getId())))
                .toList();
    }

    @Override
    public VendorResponse getById(Long id) {
        return repository.findById(id)
                .map(vendor -> toResponse(vendor, findBankDetails(vendor.getId())))
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
    }

    private BankAccountResponse createBankDetailsIfPresent(
            Vendor vendor,
            VendorBankDetailsRequest bankDetails
    ) {
        if (bankDetails == null || !bankDetails.hasValues()) return null;
        return bankAccountService.create(toBankAccountRequest(vendor, bankDetails, null));
    }

    private BankAccountResponse synchronizeBankDetails(
            Vendor vendor,
            VendorBankDetailsRequest bankDetails
    ) {
        BankAccountResponse existing = findBankDetails(vendor.getId());
        if (bankDetails == null || !bankDetails.hasValues()) return existing;
        BankAccountRequest bankRequest = toBankAccountRequest(vendor, bankDetails, existing);
        return existing == null
                ? bankAccountService.create(bankRequest)
                : bankAccountService.update(existing.getId(), bankRequest);
    }

    private BankAccountResponse findBankDetails(Long vendorId) {
        List<BankAccountResponse> accounts = bankAccountService.getAll(
                BankAccountOwnerType.VENDOR, vendorId);
        if (accounts == null || accounts.isEmpty()) return null;
        return accounts.stream()
                .filter(BankAccountResponse::isPrimary)
                .findFirst()
                .orElse(accounts.get(0));
    }

    private BankAccountRequest toBankAccountRequest(
            Vendor vendor,
            VendorBankDetailsRequest bankDetails,
            BankAccountResponse existing
    ) {
        String accountNumber = trimToNull(bankDetails.getAccountNumber());
        boolean onsite = isOnsite(vendor, bankDetails);
        return BankAccountRequest.builder()
                .ownerType(BankAccountOwnerType.VENDOR)
                .ownerId(vendor.getId())
                .bankCountry(onsite ? "US" : "IN")
                .currency(onsite ? "USD" : "INR")
                .paymentMethod(trimToNull(bankDetails.getPaymentMethod()) == null
                        ? onsite ? "ACH" : "Bank Transfer" : bankDetails.getPaymentMethod())
                .accountType(trimToNull(bankDetails.getAccountType()) == null
                        ? onsite ? "Checking" : "Current" : bankDetails.getAccountType())
                .accountHolderName(firstPresent(bankDetails.getAccountHolderName(),
                        existing == null ? null : existing.getAccountHolderName()))
                .bankName(firstPresent(bankDetails.getBankName(),
                        existing == null ? null : existing.getBankName()))
                .accountNumberEncrypted(accountNumber)
                .accountNumberLast4(lastFour(accountNumber))
                .routingNumberEncrypted(trimToNull(bankDetails.getRoutingNumber()))
                .ifscCode(firstPresent(bankDetails.getIfscCode(),
                        existing == null ? null : existing.getIfscCode()))
                .branchName(firstPresent(bankDetails.getBranch(),
                        existing == null ? null : existing.getBranchName()))
                .remittanceEmail(firstPresent(bankDetails.getRemittanceEmail(),
                        existing == null ? null : existing.getRemittanceEmail()))
                .isPrimary(true)
                .active(true)
                .build();
    }

    private VendorResponse toResponse(Vendor vendor, BankAccountResponse bankDetails) {
        VendorResponse response = VendorMapper.toResponse(vendor);
        response.setBankDetails(toBankDetailsResponse(bankDetails));
        return response;
    }

    private VendorBankDetailsResponse toBankDetailsResponse(
            BankAccountResponse bankDetails
    ) {
        if (bankDetails == null) return null;
        return VendorBankDetailsResponse.builder()
                .id(bankDetails.getId())
                .bankCountry(bankDetails.getBankCountry())
                .currency(bankDetails.getCurrency())
                .paymentMethod(bankDetails.getPaymentMethod())
                .accountType(bankDetails.getAccountType())
                .accountHolderName(bankDetails.getAccountHolderName())
                .bankName(bankDetails.getBankName())
                .accountNumberLast4(bankDetails.getAccountNumberLast4())
                .routingNumberLast4(bankDetails.getRoutingNumberLast4())
                .ifscCode(bankDetails.getIfscCode())
                .branch(bankDetails.getBranchName())
                .remittanceEmail(bankDetails.getRemittanceEmail())
                .build();
    }

    private String lastFour(String value) {
        if (value == null) return null;
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private void apply(Vendor vendor, VendorRequest request) {
        vendor.setCompanyName(request.getCompanyName().trim());
        String vendorLocation = normalizeVendorLocation(request.getVendorLocation());
        String taxIdentifier = normalizeTaxIdentifier(request.getTaxIdentifier());
        validateTaxIdentifierFormat(vendorLocation, taxIdentifier);
        vendor.setVendorLocation(vendorLocation);
        vendor.setVendorType(normalizeUpper(request.getVendorType()));
        vendor.setTaxIdentifier(taxIdentifier);
        vendor.setPrimaryContact(trimToNull(request.getPrimaryContact()));
        vendor.setContactEmail(normalizeEmail(request.getContactEmail()));
        vendor.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        vendor.setPaymentTerms(trimToNull(request.getPaymentTerms()));
        vendor.setCurrency(trimToNull(request.getCurrency()));
        vendor.setStatus(normalizeStatus(request.getStatus()));
        vendor.setAddress(trimToNull(request.getAddress()));
        if (request.getDocumentList() != null) {
            synchronizeDocuments(vendor, request.getDocumentList());
        }
    }

    private void synchronizeDocuments(Vendor vendor, List<VendorDocumentRequest> documentList) {
        List<Long> requestedIds = documentList.stream()
                .map(VendorDocumentRequest::getId)
                .toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidOperationException(
                    "documentList cannot contain duplicate document ids");
        }

        List<Document> documents = uniqueIds.isEmpty()
                ? List.of() : documentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(uniqueIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Documents not found: " + missingIds);
        }

        vendor.getDocuments().clear();
        vendor.getDocuments().addAll(documents);
    }

    private void validateTaxIdentifierAvailable(Long vendorId, String value) {
        String taxIdentifier = normalizeTaxIdentifier(value);
        if (taxIdentifier == null) return;
        boolean exists = vendorId == null
                ? repository.existsByTaxIdentifierIgnoreCase(taxIdentifier)
                : repository.existsByTaxIdentifierIgnoreCaseAndIdNot(taxIdentifier, vendorId);
        if (exists) {
            throw new DuplicateResourceException("Vendor tax identifier already exists: " + taxIdentifier);
        }
    }

    private void validateBankDetails(
            Vendor vendor,
            VendorBankDetailsRequest bankDetails,
            boolean updating
    ) {
        if (bankDetails == null || !bankDetails.hasValues()) return;
        boolean onsite = isOnsite(vendor, bankDetails);
        boolean existingAccount = updating && findBankDetails(vendor.getId()) != null;
        if (!existingAccount && trimToNull(bankDetails.getAccountHolderName()) == null) {
            throw new InvalidOperationException("bankDetails.accountHolderName is required");
        }
        if (!existingAccount && trimToNull(bankDetails.getBankName()) == null) {
            throw new InvalidOperationException("bankDetails.bankName is required");
        }
        if (!existingAccount && trimToNull(bankDetails.getAccountNumber()) == null) {
            throw new InvalidOperationException("bankDetails.accountNumber is required");
        }
        if (onsite && !existingAccount && trimToNull(bankDetails.getRoutingNumber()) == null) {
            throw new InvalidOperationException("bankDetails.routingNumber is required for Onsite vendors");
        }
        if (!onsite && trimToNull(bankDetails.getIfscCode()) == null) {
            throw new InvalidOperationException("bankDetails.ifscCode is required for Offshore vendors");
        }
    }

    private boolean isOnsite(Vendor vendor, VendorBankDetailsRequest bankDetails) {
        if (vendor.getVendorLocation() != null) {
            return "ONSITE".equals(vendor.getVendorLocation());
        }
        return !"IN".equalsIgnoreCase(bankDetails.getBankCountry());
    }

    private String normalizeVendorLocation(String value) {
        String normalized = normalizeUpper(value);
        if (normalized == null) return null;
        if (!Set.of("ONSITE", "OFFSHORE").contains(normalized)) {
            throw new InvalidOperationException("vendorLocation must be ONSITE or OFFSHORE");
        }
        return normalized;
    }

    private String normalizeTaxIdentifier(String value) {
        String normalized = normalizeUpper(value);
        return normalized == null ? null : normalized.replace(" ", "");
    }

    private void validateTaxIdentifierFormat(String vendorLocation, String taxIdentifier) {
        if (vendorLocation == null || taxIdentifier == null) return;
        if ("ONSITE".equals(vendorLocation) && !taxIdentifier.matches("\\d{2}-\\d{7}")) {
            throw new InvalidOperationException("taxIdentifier must use EIN format 12-3456789 for Onsite vendors");
        }
        if ("OFFSHORE".equals(vendorLocation)
                && !taxIdentifier.matches("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]")) {
            throw new InvalidOperationException("taxIdentifier must be a valid 15-character GST number for Offshore vendors");
        }
    }

    private String normalizeUpper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String firstPresent(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new InvalidOperationException("status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
