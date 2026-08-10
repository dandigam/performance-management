package com.rit.performance.controller;

import com.rit.performance.dto.BankAccountRequest;
import com.rit.performance.dto.BankAccountResponse;
import com.rit.performance.dto.BankAccountSensitiveDetailsResponse;
import com.rit.performance.entity.BankAccountOwnerType;
import com.rit.performance.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountService service;

    @PostMapping
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody BankAccountRequest request) {
        BankAccountResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/bank-accounts/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // Temporary public endpoint. Restrict to authorized finance/admin users when
    // application authentication is integrated.
    @GetMapping("/{id}/sensitive-details")
    public ResponseEntity<BankAccountSensitiveDetailsResponse> getSensitiveDetails(
            @PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.getSensitiveDetails(id));
    }

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> getAll(
            @RequestParam(required = false) BankAccountOwnerType ownerType,
            @RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(service.getAll(ownerType, ownerId));
    }
}
