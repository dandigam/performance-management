package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.AuditResponse;
import com.rit.performance.dto.response.SowInvoiceResponse;
import com.rit.performance.dto.request.SowInvoicePaymentRequest;
import com.rit.performance.dto.response.SowInvoicePaymentResponse;
import com.rit.performance.dto.response.SowInvoiceHistoryResponse;
import com.rit.performance.dto.response.SowInvoicePaymentHistoryResponse;
import com.rit.performance.dto.response.SowInvoiceAuditHistoryResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowInvoice;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.entity.SowInvoicePayment;
import com.rit.performance.entity.SowInvoiceHistory;
import com.rit.performance.entity.SowInvoicePaymentHistory;
import com.rit.performance.entity.User;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.SowInvoiceRepository;
import com.rit.performance.repository.SowInvoicePaymentRepository;
import com.rit.performance.repository.SowInvoiceHistoryRepository;
import com.rit.performance.repository.SowInvoicePaymentHistoryRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.mapper.AuditMapper;
import com.rit.performance.entity.BaseEntity;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.service.SowInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SowInvoiceServiceImpl implements SowInvoiceService {
    private static final Set<String> INVOICE_STATUSES = Set.of(
            "EXPECTED", "DRAFT", "RAISED", "SUBMITTED", "CANCELLED");
    private static final Set<String> PAYMENT_STATUSES = Set.of(
            "NOT_APPLICABLE", "UNPAID", "PARTIALLY_PAID", "PAID", "OVERPAID");

    private final SowInvoiceRepository invoiceRepository;
    private final SowInvoicePaymentRepository paymentRepository;
    private final SowInvoiceHistoryRepository invoiceHistoryRepository;
    private final SowInvoicePaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
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
                        || normalizedPaymentStatus.equals(paymentStatus(invoice)))
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
        SowInvoice saved = invoiceRepository.saveAndFlush(invoice);
        assignInvoiceNumber(saved);
        saved = invoiceRepository.save(saved);
        recordInvoiceHistory(saved, "CREATED", request.getUpdatedBy());
        return toResponse(saved);
    }

    @Override
    public SowInvoiceResponse update(Long id, SowInvoiceRequest request) {
        SowInvoice invoice = findInvoice(id);
        if (!invoice.getMilestone().getId().equals(request.getMilestoneId())) {
            throw new InvalidOperationException("milestoneId cannot be changed for an invoice");
        }
        String previousStatus = invoice.getInvoiceStatus();
        apply(invoice, request);
        SowInvoice saved = invoiceRepository.save(invoice);
        String action = Objects.equals(previousStatus, invoice.getInvoiceStatus())
                ? "INVOICE_UPDATED" : invoice.getInvoiceStatus();
        recordInvoiceHistory(saved, action, request.getUpdatedBy());
        return toResponse(saved);
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
        List<SowInvoice> existingInvoices = invoiceRepository.findByMilestone_IdIn(milestoneIds);
        Map<Long, SowInvoice> invoicesByMilestone = existingInvoices.stream()
                .collect(Collectors.toMap(invoice -> invoice.getMilestone().getId(),
                        invoice -> invoice));

        // Keep planned billing synchronized while the invoice is still unraised.
        // Once raised/submitted, its financial snapshot must remain unchanged.
        sowMilestones.forEach(milestone -> {
            SowInvoice invoice = invoicesByMilestone.get(milestone.getId());
            if (invoice != null && Set.of("EXPECTED", "DRAFT")
                    .contains(invoice.getInvoiceStatus())) {
                boolean changed = !Objects.equals(
                        invoice.getMilestoneInvoiceDate(), milestone.getInvoiceDate())
                        || !Objects.equals(
                        invoice.getMilestoneInvoiceAmount(), milestone.getAmount());
                invoice.setMilestoneInvoiceDate(milestone.getInvoiceDate());
                invoice.setMilestoneInvoiceAmount(milestone.getAmount());
                if (changed) recordInvoiceHistory(
                        invoice, "MILESTONE_UPDATED", milestone.getUpdatedBy());
            }
        });
        if (!existingInvoices.isEmpty()) invoiceRepository.saveAll(existingInvoices);

        List<SowInvoice> missingInvoices = sowMilestones.stream()
                .filter(milestone -> !invoicesByMilestone.containsKey(milestone.getId()))
                .<SowInvoice>map(milestone -> SowInvoice.builder()
                        .sow(sow)
                        .milestone(milestone)
                        .milestoneInvoiceDate(milestone.getInvoiceDate())
                        .milestoneInvoiceAmount(milestone.getAmount())
                        .invoiceStatus("DRAFT")
                        .build())
                .toList();
        List<SowInvoice> savedInvoices = invoiceRepository.saveAllAndFlush(missingInvoices);
        savedInvoices.forEach(this::assignInvoiceNumber);
        invoiceRepository.saveAll(savedInvoices);
        savedInvoices.forEach(invoice -> recordInvoiceHistory(
                invoice, "CREATED", invoice.getCreatedBy()));
    }

    private void apply(SowInvoice invoice, SowInvoiceRequest request) {
        if (request.getMilestoneInvoiceDate() != null) {
            invoice.setMilestoneInvoiceDate(request.getMilestoneInvoiceDate());
        } else if (invoice.getMilestoneInvoiceDate() == null) {
            invoice.setMilestoneInvoiceDate(invoice.getMilestone().getInvoiceDate());
        }
        if (request.getMilestoneInvoiceAmount() != null) {
            invoice.setMilestoneInvoiceAmount(request.getMilestoneInvoiceAmount());
        } else if (invoice.getMilestoneInvoiceAmount() == null) {
            invoice.setMilestoneInvoiceAmount(invoice.getMilestone().getAmount());
        }
        invoice.setInvoiceRaisedDate(request.getInvoiceRaisedDate());
        invoice.setInvoiceRaisedAmount(request.getInvoiceRaisedAmount());
        String invoiceStatus = normalizeStatus(
                request.getInvoiceStatus(), INVOICE_STATUSES, "invoiceStatus", "EXPECTED");
        invoice.setInvoiceStatus(invoiceStatus);
        LocalDate submittedDate = request.getSubmittedDate();
        if (submittedDate == null && "SUBMITTED".equals(invoiceStatus)) {
            submittedDate = LocalDate.now();
        }
        invoice.setSubmittedDate(submittedDate);
        invoice.setNotes(trimToNull(request.getNotes()));
        invoice.setUpdatedBy(request.getUpdatedBy());
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

    @Override
    @Transactional(readOnly = true)
    public List<SowInvoicePaymentResponse> getPayments(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("SOW invoice not found: " + invoiceId);
        }
        return paymentRepository.findByInvoice_IdOrderByPaymentDateAscIdAsc(invoiceId)
                .stream().map(this::toPaymentResponse).toList();
    }

    @Override
    public SowInvoicePaymentResponse createPayment(
            Long invoiceId, SowInvoicePaymentRequest request) {
        SowInvoice invoice = findInvoice(invoiceId);
        SowInvoicePayment payment = new SowInvoicePayment();
        payment.setInvoice(invoice);
        payment.setCreatedBy(request.getUpdatedBy());
        applyPayment(payment, request);
        SowInvoicePayment saved = paymentRepository.save(payment);
        recordPaymentHistory(saved, "CREATED", request.getUpdatedBy());
        return toPaymentResponse(saved);
    }

    @Override
    public SowInvoicePaymentResponse updatePayment(
            Long invoiceId, Long paymentId, SowInvoicePaymentRequest request) {
        SowInvoicePayment payment = findPayment(invoiceId, paymentId);
        applyPayment(payment, request);
        SowInvoicePayment saved = paymentRepository.save(payment);
        recordPaymentHistory(saved, "UPDATED", request.getUpdatedBy());
        return toPaymentResponse(saved);
    }

    @Override
    public void deletePayment(Long invoiceId, Long paymentId) {
        SowInvoicePayment payment = findPayment(invoiceId, paymentId);
        recordPaymentHistory(payment, "DELETED", payment.getUpdatedBy());
        paymentRepository.delete(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowInvoiceHistoryResponse> getHistory(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("SOW invoice not found: " + invoiceId);
        }
        return invoiceHistoryRepository.findByInvoice_IdOrderByChangedOnDescIdDesc(invoiceId)
                .stream().map(this::toHistoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowInvoicePaymentHistoryResponse> getPaymentHistory(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("SOW invoice not found: " + invoiceId);
        }
        return paymentHistoryRepository.findByInvoice_IdOrderByChangedOnDescIdDesc(invoiceId)
                .stream().map(this::toPaymentHistoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SowInvoiceAuditHistoryResponse getAuditHistory(Long invoiceId) {
        SowInvoice invoice = findInvoice(invoiceId);
        List<SowInvoiceHistoryResponse> invoiceHistory = invoiceHistoryRepository
                .findByInvoice_IdOrderByChangedOnDescIdDesc(invoiceId)
                .stream().map(this::toHistoryResponse).toList();
        List<SowInvoicePaymentHistoryResponse> paymentHistory = paymentHistoryRepository
                .findByInvoice_IdOrderByChangedOnDescIdDesc(invoiceId)
                .stream().map(this::toPaymentHistoryResponse).toList();
        return SowInvoiceAuditHistoryResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDetails(toResponse(invoice))
                .invoiceHistory(invoiceHistory)
                .paymentHistory(paymentHistory)
                .build();
    }

    private SowInvoicePayment findPayment(Long invoiceId, Long paymentId) {
        return paymentRepository.findByIdAndInvoice_Id(paymentId, invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment " + paymentId + " not found for invoice " + invoiceId));
    }

    private void applyPayment(SowInvoicePayment payment, SowInvoicePaymentRequest request) {
        payment.setPaymentDate(request.getPaymentDate());
        payment.setReceivedAmount(request.getReceivedAmount());
        payment.setPaymentReference(trimToNull(request.getPaymentReference()));
        payment.setPaymentMethod("PAYMENT_RECEIVED");
        payment.setNotes(trimToNull(request.getNotes()));
        payment.setUpdatedBy(request.getUpdatedBy());
    }

    private SowInvoiceResponse toResponse(SowInvoice invoice) {
        Sow sow = invoice.getSow();
        SowMilestone milestone = invoice.getMilestone();
        LookupValue department = sow.getBusinessUnit();
        List<SowInvoicePaymentResponse> payments = invoice.getPayments().stream()
                .sorted(Comparator.comparing(SowInvoicePayment::getPaymentDate)
                        .thenComparing(SowInvoicePayment::getId))
                .map(this::toPaymentResponse)
                .toList();
        BigDecimal totalReceived = payments.stream()
                .map(SowInvoicePaymentResponse::getReceivedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balanceAmount = invoice.getInvoiceRaisedAmount() == null
                ? null : invoice.getInvoiceRaisedAmount().subtract(totalReceived);
        return SowInvoiceResponse.builder()
                .id(invoice.getId())
                .departmentId(department == null ? null : department.getId())
                .departmentName(department == null ? null : department.getName())
                .sowId(sow.getId()).sowCode(sow.getSowCode()).sowName(sow.getSowName())
                .milestoneId(milestone.getId()).milestoneName(milestone.getMilestoneName())
                .expectedCompletionDate(milestone.getEndDate())
                .milestoneInvoiceDate(invoice.getMilestoneInvoiceDate())
                .milestoneInvoiceAmount(invoice.getMilestoneInvoiceAmount())
                .invoiceRaisedDate(invoice.getInvoiceRaisedDate())
                .invoiceRaisedAmount(invoice.getInvoiceRaisedAmount())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceStatus(invoice.getInvoiceStatus())
                .submittedDate(invoice.getSubmittedDate())
                .notes(invoice.getNotes())
                .totalReceived(totalReceived)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus(invoice.getInvoiceRaisedAmount(), totalReceived))
                .payments(payments)
                .createdBy(invoice.getCreatedBy()).createdDate(invoice.getCreatedOn())
                .updatedBy(invoice.getUpdatedBy()).updatedDate(invoice.getUpdatedOn())
                .audit(audit(invoice))
                .build();
    }

    private String paymentStatus(SowInvoice invoice) {
        BigDecimal total = invoice.getPayments().stream()
                .map(SowInvoicePayment::getReceivedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return paymentStatus(invoice.getInvoiceRaisedAmount(), total);
    }

    private String paymentStatus(BigDecimal invoiceRaisedAmount, BigDecimal totalReceived) {
        if (invoiceRaisedAmount == null) return "NOT_APPLICABLE";
        int comparison = totalReceived.compareTo(invoiceRaisedAmount);
        if (totalReceived.signum() == 0) return "UNPAID";
        if (comparison < 0) return "PARTIALLY_PAID";
        if (comparison == 0) return "PAID";
        return "OVERPAID";
    }

    private SowInvoicePaymentResponse toPaymentResponse(SowInvoicePayment payment) {
        return SowInvoicePaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .paymentDate(payment.getPaymentDate())
                .receivedAmount(payment.getReceivedAmount())
                .paymentReference(payment.getPaymentReference())
                .paymentMethod(payment.getPaymentMethod())
                .notes(payment.getNotes())
                .createdBy(payment.getCreatedBy()).createdDate(payment.getCreatedOn())
                .updatedBy(payment.getUpdatedBy()).updatedDate(payment.getUpdatedOn())
                .audit(audit(payment))
                .build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void assignInvoiceNumber(SowInvoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber("INV-%06d".formatted(invoice.getId()));
        }
    }

    private void recordInvoiceHistory(SowInvoice invoice, String action, Long changedBy) {
        invoiceHistoryRepository.save(SowInvoiceHistory.builder()
                .invoice(invoice)
                .milestoneInvoiceDate(invoice.getMilestoneInvoiceDate())
                .milestoneInvoiceAmount(invoice.getMilestoneInvoiceAmount())
                .invoiceRaisedDate(invoice.getInvoiceRaisedDate())
                .invoiceRaisedAmount(invoice.getInvoiceRaisedAmount())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceStatus(invoice.getInvoiceStatus())
                .submittedDate(invoice.getSubmittedDate())
                .notes(invoice.getNotes())
                .action(action)
                .changedBy(changedBy)
                .changedOn(LocalDateTime.now())
                .build());
    }

    private void recordPaymentHistory(
            SowInvoicePayment payment, String action, Long changedBy) {
        paymentHistoryRepository.save(SowInvoicePaymentHistory.builder()
                .invoice(payment.getInvoice())
                .paymentId(payment.getId())
                .paymentDate(payment.getPaymentDate())
                .receivedAmount(payment.getReceivedAmount())
                .paymentReference(payment.getPaymentReference())
                .paymentMethod(payment.getPaymentMethod())
                .notes(payment.getNotes())
                .action(action)
                .changedBy(changedBy)
                .changedOn(LocalDateTime.now())
                .build());
    }

    private SowInvoiceHistoryResponse toHistoryResponse(SowInvoiceHistory history) {
        SowInvoice invoice = history.getInvoice();
        return SowInvoiceHistoryResponse.builder()
                .id(history.getId()).invoiceId(invoice.getId())
                .sowId(invoice.getSow().getId()).sowCode(invoice.getSow().getSowCode())
                .sowName(invoice.getSow().getSowName())
                .milestoneId(invoice.getMilestone().getId())
                .milestoneName(invoice.getMilestone().getMilestoneName())
                .milestoneInvoiceDate(history.getMilestoneInvoiceDate())
                .milestoneInvoiceAmount(history.getMilestoneInvoiceAmount())
                .invoiceRaisedDate(history.getInvoiceRaisedDate())
                .invoiceRaisedAmount(history.getInvoiceRaisedAmount())
                .invoiceNumber(history.getInvoiceNumber())
                .invoiceStatus(history.getInvoiceStatus())
                .submittedDate(history.getSubmittedDate()).notes(history.getNotes())
                .action(history.getAction()).changedBy(history.getChangedBy())
                .changedByName(userDisplayName(history.getChangedBy()))
                .changedOn(history.getChangedOn()).build();
    }

    private SowInvoicePaymentHistoryResponse toPaymentHistoryResponse(
            SowInvoicePaymentHistory history) {
        SowInvoice invoice = history.getInvoice();
        return SowInvoicePaymentHistoryResponse.builder()
                .id(history.getId()).invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .sowId(invoice.getSow().getId()).sowCode(invoice.getSow().getSowCode())
                .sowName(invoice.getSow().getSowName())
                .milestoneId(invoice.getMilestone().getId())
                .milestoneName(invoice.getMilestone().getMilestoneName())
                .paymentId(history.getPaymentId()).paymentDate(history.getPaymentDate())
                .receivedAmount(history.getReceivedAmount())
                .paymentReference(history.getPaymentReference())
                .paymentMethod(history.getPaymentMethod()).notes(history.getNotes())
                .action(history.getAction()).changedBy(history.getChangedBy())
                .changedByName(userDisplayName(history.getChangedBy()))
                .changedOn(history.getChangedOn()).build();
    }

    private String userDisplayName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(this::userDisplayName).orElse(null);
    }

    private String userDisplayName(User user) {
        if (user.getEmployee() == null) return user.getUsername();
        String firstName = user.getEmployee().getFirstName() == null
                ? "" : user.getEmployee().getFirstName().trim();
        String lastName = user.getEmployee().getLastName() == null
                ? "" : user.getEmployee().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }

    private AuditResponse audit(BaseEntity entity) {
        Map<Long, String> names = new HashMap<>();
        if (entity.getCreatedBy() != null) {
            names.put(entity.getCreatedBy(), userDisplayName(entity.getCreatedBy()));
        }
        if (entity.getUpdatedBy() != null) {
            names.put(entity.getUpdatedBy(), userDisplayName(entity.getUpdatedBy()));
        }
        return AuditMapper.toResponse(entity, names);
    }
}
