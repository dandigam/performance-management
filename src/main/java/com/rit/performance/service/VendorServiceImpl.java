package com.rit.performance.service;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.VendorRequest;
import com.rit.performance.dto.VendorResponse;
import com.rit.performance.dto.VendorDocumentRequest;
import com.rit.performance.dto.VendorPaymentDetailsRequest;
import com.rit.performance.dto.VendorPaymentDetailsResponse;
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
        String vendorCode = normalizeCode(request.getVendorCode());
        if (repository.existsByVendorCodeIgnoreCase(vendorCode)) {
            throw new DuplicateResourceException("Vendor code already exists: " + vendorCode);
        }

        Vendor vendor = new Vendor();
        apply(vendor, request, vendorCode);
        vendor = repository.save(vendor);
        BankAccountResponse paymentDetails = createPaymentDetailsIfPresent(
                vendor.getId(), request.getPaymentDetails());
        return toResponse(vendor, paymentDetails);
    }

    @Override
    @Transactional
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor vendor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
        String vendorCode = normalizeCode(request.getVendorCode());
        if (repository.existsByVendorCodeIgnoreCaseAndIdNot(vendorCode, id)) {
            throw new DuplicateResourceException("Vendor code already exists: " + vendorCode);
        }

        apply(vendor, request, vendorCode);
        vendor = repository.save(vendor);
        BankAccountResponse paymentDetails = synchronizePaymentDetails(
                vendor.getId(), request.getPaymentDetails());
        return toResponse(vendor, paymentDetails);
    }

    @Override
    public List<VendorResponse> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "companyName", "vendorCode")).stream()
                .map(vendor -> toResponse(vendor, findPaymentDetails(vendor.getId())))
                .toList();
    }

    @Override
    public VendorResponse getById(Long id) {
        return repository.findById(id)
                .map(vendor -> toResponse(vendor, findPaymentDetails(vendor.getId())))
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
    }

    private BankAccountResponse createPaymentDetailsIfPresent(
            Long vendorId,
            VendorPaymentDetailsRequest paymentDetails
    ) {
        if (paymentDetails == null || !paymentDetails.hasValues()) return null;
        return bankAccountService.create(toBankAccountRequest(vendorId, paymentDetails));
    }

    private BankAccountResponse synchronizePaymentDetails(
            Long vendorId,
            VendorPaymentDetailsRequest paymentDetails
    ) {
        BankAccountResponse existing = findPaymentDetails(vendorId);
        if (paymentDetails == null || !paymentDetails.hasValues()) return existing;
        BankAccountRequest bankRequest = toBankAccountRequest(vendorId, paymentDetails);
        return existing == null
                ? bankAccountService.create(bankRequest)
                : bankAccountService.update(existing.getId(), bankRequest);
    }

    private BankAccountResponse findPaymentDetails(Long vendorId) {
        List<BankAccountResponse> accounts = bankAccountService.getAll(
                BankAccountOwnerType.VENDOR, vendorId);
        if (accounts == null || accounts.isEmpty()) return null;
        return accounts.stream()
                .filter(BankAccountResponse::isPrimary)
                .findFirst()
                .orElse(accounts.get(0));
    }

    private BankAccountRequest toBankAccountRequest(
            Long vendorId,
            VendorPaymentDetailsRequest paymentDetails
    ) {
        String accountNumber = trimToNull(paymentDetails.getAccountNumber());
        return BankAccountRequest.builder()
                .ownerType(BankAccountOwnerType.VENDOR)
                .ownerId(vendorId)
                .bankCountry(paymentDetails.getBankCountry())
                .currency(paymentDetails.getCurrency())
                .paymentMethod(paymentDetails.getPaymentMethod())
                .accountType(paymentDetails.getAccountType())
                .accountHolderName(paymentDetails.getAccountHolderName())
                .bankName(paymentDetails.getBankName())
                .accountNumberEncrypted(accountNumber)
                .accountNumberLast4(lastFour(accountNumber))
                .routingNumberEncrypted(trimToNull(paymentDetails.getRoutingNumber()))
                .ifscCode(paymentDetails.getIfscCode())
                .branchName(paymentDetails.getBranch())
                .remittanceEmail(paymentDetails.getRemittanceEmail())
                .isPrimary(true)
                .active(true)
                .build();
    }

    private VendorResponse toResponse(Vendor vendor, BankAccountResponse paymentDetails) {
        VendorResponse response = VendorMapper.toResponse(vendor);
        response.setPaymentDetails(toPaymentDetailsResponse(paymentDetails));
        return response;
    }

    private VendorPaymentDetailsResponse toPaymentDetailsResponse(
            BankAccountResponse paymentDetails
    ) {
        if (paymentDetails == null) return null;
        return VendorPaymentDetailsResponse.builder()
                .id(paymentDetails.getId())
                .bankCountry(paymentDetails.getBankCountry())
                .currency(paymentDetails.getCurrency())
                .paymentMethod(paymentDetails.getPaymentMethod())
                .accountType(paymentDetails.getAccountType())
                .accountHolderName(paymentDetails.getAccountHolderName())
                .bankName(paymentDetails.getBankName())
                .accountNumberLast4(paymentDetails.getAccountNumberLast4())
                .routingNumberLast4(paymentDetails.getRoutingNumberLast4())
                .ifscCode(paymentDetails.getIfscCode())
                .branch(paymentDetails.getBranchName())
                .remittanceEmail(paymentDetails.getRemittanceEmail())
                .build();
    }

    private String lastFour(String value) {
        if (value == null) return null;
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private void apply(Vendor vendor, VendorRequest request, String vendorCode) {
        vendor.setVendorCode(vendorCode);
        vendor.setCompanyName(request.getCompanyName().trim());
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

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
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
