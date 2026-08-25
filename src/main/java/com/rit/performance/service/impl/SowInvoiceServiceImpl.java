package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.response.SowInvoiceResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowInvoice;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.SowInvoiceRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.service.SowInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SowInvoiceServiceImpl implements SowInvoiceService {
    private static final Set<String> INVOICE_STATUSES = Set.of(
            "DRAFT", "SUBMITTED", "SENT", "CANCELLED");
    private static final Set<String> PAYMENT_STATUSES = Set.of(
            "UNPAID", "PARTIALLY_PAID", "PAID");

    private final SowInvoiceRepository invoiceRepository;
    private final SowMilestoneRepository milestoneRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SowInvoiceResponse> getAll(Long sowId, String invoiceStatus, String paymentStatus) {
        String normalizedInvoiceStatus = normalizeFilter(invoiceStatus, INVOICE_STATUSES, "invoiceStatus");
        String normalizedPaymentStatus = normalizeFilter(paymentStatus, PAYMENT_STATUSES, "paymentStatus");
        return invoiceRepository.findAllWithDetails().stream()
                .filter(invoice -> sowId == null || sowId.equals(invoice.getSow().getId()))
                .filter(invoice -> normalizedInvoiceStatus == null
                        || normalizedInvoiceStatus.equals(invoice.getInvoiceStatus()))
                .filter(invoice -> normalizedPaymentStatus == null
                        || normalizedPaymentStatus.equals(invoice.getPaymentStatus()))
                .sorted(Comparator
                        .comparing((SowInvoice invoice) -> invoice.getMilestone().getInvoiceDate(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SowInvoice::getId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SowInvoiceResponse getById(Long id) {
        return toResponse(findInvoice(id));
    }

    @Override
    public SowInvoiceResponse create(SowInvoiceRequest request) {
        SowMilestone milestone = milestoneRepository.findById(request.getMilestoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SOW milestone not found: " + request.getMilestoneId()));
        if (invoiceRepository.existsByMilestone_Id(milestone.getId())) {
            throw new DuplicateResourceException(
                    "Invoice already exists for milestone " + milestone.getId());
        }
        SowInvoice invoice = new SowInvoice();
        invoice.setSow(milestone.getSow());
        invoice.setMilestone(milestone);
        invoice.setCreatedBy(request.getUpdatedBy());
        apply(invoice, request);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public SowInvoiceResponse update(Long id, SowInvoiceRequest request) {
        SowInvoice invoice = findInvoice(id);
        if (!invoice.getMilestone().getId().equals(request.getMilestoneId())) {
            throw new InvalidOperationException("milestoneId cannot be changed for an invoice");
        }
        apply(invoice, request);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public void createDraftInvoices(Sow sow, Collection<SowMilestone> milestones) {
        if (sow == null || sow.getId() == null || milestones == null || milestones.isEmpty()) return;
        List<SowMilestone> sowMilestones = milestones.stream()
                .filter(Objects::nonNull)
                .filter(milestone -> milestone.getId() != null)
                .filter(milestone -> milestone.getSow() != null
                        && sow.getId().equals(milestone.getSow().getId()))
                .toList();
        List<Long> milestoneIds = sowMilestones.stream().map(SowMilestone::getId).toList();
        if (milestoneIds.isEmpty()) return;
        Set<Long> invoicedMilestoneIds = invoiceRepository.findByMilestone_IdIn(milestoneIds).stream()
                .map(invoice -> invoice.getMilestone().getId())
                .collect(Collectors.toSet());
        List<SowInvoice> missingInvoices = sowMilestones.stream()
                .filter(milestone -> !invoicedMilestoneIds.contains(milestone.getId()))
                .map(milestone -> SowInvoice.builder()
                        .sow(sow)
                        .milestone(milestone)
                        .invoiceStatus("DRAFT")
                        .paymentStatus("UNPAID")
                        .build())
                .toList();
        invoiceRepository.saveAll(missingInvoices);
    }

    private void apply(SowInvoice invoice, SowInvoiceRequest request) {
        invoice.setActualInvoiceDate(request.getActualInvoiceDate());
        invoice.setInvoiceAmount(request.getInvoiceAmount());
        String invoiceStatus = normalizeStatus(
                request.getInvoiceStatus(), INVOICE_STATUSES, "invoiceStatus", "DRAFT");
        invoice.setInvoiceStatus(invoiceStatus);
        LocalDate submittedDate = request.getSubmittedDate();
        if (submittedDate == null && Set.of("SUBMITTED", "SENT").contains(invoiceStatus)) {
            submittedDate = LocalDate.now();
        }
        invoice.setSubmittedDate(submittedDate);
        invoice.setPaymentReceivedDate(request.getPaymentReceivedDate());
        invoice.setReceivedAmount(request.getReceivedAmount());
        invoice.setPaymentStatus(resolvePaymentStatus(request));
        invoice.setUpdatedBy(request.getUpdatedBy());
    }

    private String resolvePaymentStatus(SowInvoiceRequest request) {
        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            return normalizeStatus(request.getPaymentStatus(), PAYMENT_STATUSES, "paymentStatus", "UNPAID");
        }
        BigDecimal received = request.getReceivedAmount();
        if (received == null || received.signum() == 0) return "UNPAID";
        BigDecimal invoiceAmount = request.getInvoiceAmount();
        return invoiceAmount != null && received.compareTo(invoiceAmount) >= 0
                ? "PAID" : "PARTIALLY_PAID";
    }

    private String normalizeFilter(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) return null;
        return normalizeStatus(value, allowed, field, null);
    }

    private String normalizeStatus(
            String value, Set<String> allowed, String field, String defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!allowed.contains(normalized)) {
            throw new InvalidOperationException(field + " must be one of " + allowed);
        }
        return normalized;
    }

    private SowInvoice findInvoice(Long id) {
        return invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOW invoice not found: " + id));
    }

    private SowInvoiceResponse toResponse(SowInvoice invoice) {
        Sow sow = invoice.getSow();
        SowMilestone milestone = invoice.getMilestone();
        LookupValue department = sow.getBusinessUnit();
        return SowInvoiceResponse.builder()
                .id(invoice.getId())
                .departmentId(department == null ? null : department.getId())
                .departmentName(department == null ? null : department.getName())
                .sowId(sow.getId()).sowCode(sow.getSowCode()).sowName(sow.getSowName())
                .milestoneId(milestone.getId()).milestoneName(milestone.getMilestoneName())
                .expectedCompletionDate(milestone.getEndDate())
                .expectedInvoiceDate(milestone.getInvoiceDate())
                .expectedAmount(milestone.getAmount())
                .actualInvoiceDate(invoice.getActualInvoiceDate())
                .invoiceAmount(invoice.getInvoiceAmount())
                .invoiceStatus(invoice.getInvoiceStatus())
                .submittedDate(invoice.getSubmittedDate())
                .paymentReceivedDate(invoice.getPaymentReceivedDate())
                .receivedAmount(invoice.getReceivedAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .createdBy(invoice.getCreatedBy()).createdDate(invoice.getCreatedDate())
                .updatedBy(invoice.getUpdatedBy()).updatedDate(invoice.getUpdatedDate())
                .build();
    }
}
