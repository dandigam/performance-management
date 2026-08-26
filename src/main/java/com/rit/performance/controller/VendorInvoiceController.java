package com.rit.performance.controller;

import com.rit.performance.dto.request.VendorInvoiceRequest;
import com.rit.performance.dto.response.VendorInvoiceResponse;
import com.rit.performance.service.VendorInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vendor-invoices")
@RequiredArgsConstructor
public class VendorInvoiceController {
    private final VendorInvoiceService vendorInvoiceService;

    @PostMapping
    public ResponseEntity<VendorInvoiceResponse> create(
            @Valid @RequestBody VendorInvoiceRequest request) {
        VendorInvoiceResponse created = vendorInvoiceService.create(request);
        return ResponseEntity.created(
                URI.create("/api/v1/vendor-invoices/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<VendorInvoiceResponse>> getAll() {
        return ResponseEntity.ok(vendorInvoiceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorInvoiceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorInvoiceResponse> update(
            @PathVariable Long id, @Valid @RequestBody VendorInvoiceRequest request) {
        return ResponseEntity.ok(vendorInvoiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vendorInvoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
