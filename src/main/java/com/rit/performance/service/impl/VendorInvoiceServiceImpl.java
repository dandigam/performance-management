package com.rit.performance.service.impl;

import com.rit.performance.dto.request.VendorInvoiceRequest;
import com.rit.performance.dto.response.VendorInvoiceResponse;
import com.rit.performance.dto.response.VendorInvoiceItemResponse;
import com.rit.performance.dto.request.VendorInvoiceItemRequest;
import com.rit.performance.entity.Vendor;
import com.rit.performance.entity.VendorInvoice;
import com.rit.performance.entity.VendorInvoiceItem;
import com.rit.performance.entity.WorkOrder;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.VendorInvoiceRepository;
import com.rit.performance.repository.VendorRepository;
import com.rit.performance.repository.WorkOrderRepository;
import com.rit.performance.service.VendorInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class VendorInvoiceServiceImpl implements VendorInvoiceService {
    private final VendorInvoiceRepository vendorInvoiceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final VendorRepository vendorRepository;

    @Override
    public VendorInvoiceResponse create(VendorInvoiceRequest request) {
        String invoiceNumber = request.getInvoiceNumber().trim();
        validateUniqueInvoiceNumber(invoiceNumber, null);
        VendorInvoice invoice = new VendorInvoice();
        apply(invoice, request, invoiceNumber);
        return toResponse(vendorInvoiceRepository.saveAndFlush(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorInvoiceResponse> getAll() {
        return vendorInvoiceRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorInvoiceResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    public VendorInvoiceResponse update(Long id, VendorInvoiceRequest request) {
        VendorInvoice invoice = find(id);
        String invoiceNumber = request.getInvoiceNumber().trim();
        validateUniqueInvoiceNumber(invoiceNumber, id);
        apply(invoice, request, invoiceNumber);
        return toResponse(vendorInvoiceRepository.saveAndFlush(invoice));
    }

    @Override
    public void delete(Long id) {
        vendorInvoiceRepository.delete(find(id));
    }

    private void apply(VendorInvoice invoice, VendorInvoiceRequest request, String invoiceNumber) {
        WorkOrder workOrder = workOrderRepository.findOneById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Work order not found: " + request.getWorkOrderId()));
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor not found: " + request.getVendorId()));

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setReceivedDate(request.getReceivedDate());
        invoice.setInvoiceType(request.getInvoiceType().trim().toUpperCase(Locale.ROOT));
        invoice.setWorkOrder(workOrder);
        invoice.setVendor(vendor);
        invoice.setLocation(normalizeLocation(request.getLocation()));
        invoice.setStatus(normalizeStatus(request.getStatus()));
        synchronizeItems(invoice, request.getItems());
    }

    private void synchronizeItems(
            VendorInvoice invoice, List<VendorInvoiceItemRequest> itemRequests) {
        invoice.clearItems();
        itemRequests.forEach(request -> {
            BigDecimal amount = request.getUnitPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity()));
            invoice.addItem(VendorInvoiceItem.builder()
                    .quantity(request.getQuantity())
                    .description(request.getDescription().trim())
                    .unitPrice(request.getUnitPrice())
                    .amount(amount)
                    .build());
        });
    }

    private VendorInvoice find(Long id) {
        return vendorInvoiceRepository.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor invoice not found: " + id));
    }

    private void validateUniqueInvoiceNumber(String invoiceNumber, Long currentId) {
        boolean exists = currentId == null
                ? vendorInvoiceRepository.existsByInvoiceNumberIgnoreCase(invoiceNumber)
                : vendorInvoiceRepository.existsByInvoiceNumberIgnoreCaseAndIdNot(
                        invoiceNumber, currentId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Vendor invoice number already exists: " + invoiceNumber);
        }
    }

    private String normalizeLocation(String location) {
        String normalized = location.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ONSITE", "OFFSHORE").contains(normalized)) {
            throw new InvalidOperationException("location must be ONSITE or OFFSHORE");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPT".equals(normalized)) normalized = "ACCEPTED";
        if ("REJECT".equals(normalized)) normalized = "REJECTED";
        if (!Set.of("ACCEPTED", "REJECTED").contains(normalized)) {
            throw new InvalidOperationException(
                    "status must be ACCEPTED or REJECTED");
        }
        return normalized;
    }

    private VendorInvoiceResponse toResponse(VendorInvoice invoice) {
        List<VendorInvoiceItemResponse> items = invoice.getItems().stream()
                .map(item -> VendorInvoiceItemResponse.builder()
                        .id(item.getId())
                        .quantity(item.getQuantity())
                        .description(item.getDescription())
                        .unitPrice(item.getUnitPrice())
                        .amount(item.getAmount())
                        .build())
                .toList();
        BigDecimal totalAmount = invoice.getItems().stream()
                .map(VendorInvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return VendorInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .receivedDate(invoice.getReceivedDate())
                .invoiceType(invoice.getInvoiceType())
                .workOrderId(invoice.getWorkOrder().getId())
                .workOrderNumber(invoice.getWorkOrder().getId())
                .vendorId(invoice.getVendor().getId())
                .vendorName(invoice.getVendor().getCompanyName())
                .location(invoice.getLocation())
                .status(invoice.getStatus())
                .items(items)
                .totalAmount(totalAmount)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
