package com.rit.performance.controller;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.response.SowInvoiceResponse;
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
}
