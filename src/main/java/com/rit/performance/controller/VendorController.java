package com.rit.performance.controller;

import com.rit.performance.dto.VendorRequest;
import com.rit.performance.dto.VendorResponse;
import com.rit.performance.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {
    private final VendorService service;

    @PostMapping
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorRequest request) {
        VendorResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/vendors/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
