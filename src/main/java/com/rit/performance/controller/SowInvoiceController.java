package com.rit.performance.controller;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.response.SowInvoiceResponse;
import com.rit.performance.dto.request.SowInvoicePaymentRequest;
import com.rit.performance.dto.response.SowInvoicePaymentResponse;
import com.rit.performance.dto.response.SowInvoiceHistoryResponse;
import com.rit.performance.dto.response.SowInvoicePaymentHistoryResponse;
import com.rit.performance.dto.response.SowInvoiceAuditHistoryResponse;
import com.rit.performance.service.SowInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sow-invoices")
@RequiredArgsConstructor
public class SowInvoiceController {
    private final SowInvoiceService service;

    @GetMapping
    public ResponseEntity<List<SowInvoiceResponse>> getAll(
            @RequestParam(required = false) Long sowId,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) String paymentStatus) {
        return ResponseEntity.ok(service.getAll(sowId, invoiceStatus, paymentStatus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SowInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<SowInvoiceResponse> create(@Valid @RequestBody SowInvoiceRequest request) {
        SowInvoiceResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sow-invoices/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SowInvoiceResponse> update(
            @PathVariable Long id, @Valid @RequestBody SowInvoiceRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{invoiceId}/payments")
    public ResponseEntity<List<SowInvoicePaymentResponse>> getPayments(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.getPayments(invoiceId));
    }

    @PostMapping("/{invoiceId}/payments")
    public ResponseEntity<SowInvoicePaymentResponse> createPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody SowInvoicePaymentRequest request) {
        SowInvoicePaymentResponse created = service.createPayment(invoiceId, request);
        return ResponseEntity.created(URI.create("/api/v1/sow-invoices/" + invoiceId
                + "/payments/" + created.getId())).body(created);
    }

    @PutMapping("/{invoiceId}/payments/{paymentId}")
    public ResponseEntity<SowInvoicePaymentResponse> updatePayment(
            @PathVariable Long invoiceId, @PathVariable Long paymentId,
            @Valid @RequestBody SowInvoicePaymentRequest request) {
        return ResponseEntity.ok(service.updatePayment(invoiceId, paymentId, request));
    }

    @DeleteMapping("/{invoiceId}/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long invoiceId, @PathVariable Long paymentId) {
        service.deletePayment(invoiceId, paymentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{invoiceId}/history")
    public ResponseEntity<List<SowInvoiceHistoryResponse>> getHistory(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.getHistory(invoiceId));
    }

    @GetMapping("/{invoiceId}/payments/history")
    public ResponseEntity<List<SowInvoicePaymentHistoryResponse>> getPaymentHistory(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.getPaymentHistory(invoiceId));
    }

    @GetMapping("/{invoiceId}/audit-history")
    public ResponseEntity<SowInvoiceAuditHistoryResponse> getAuditHistory(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.getAuditHistory(invoiceId));
    }
}
