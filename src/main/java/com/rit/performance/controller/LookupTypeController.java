package com.rit.performance.controller;

import com.rit.performance.dto.LookupTypeSummaryResponse;
import com.rit.performance.dto.LookupValueResponse;
import com.rit.performance.dto.LookupValueUpsertRequest;
import com.rit.performance.dto.LookupValueMutationResponse;
import jakarta.validation.Valid;
import com.rit.performance.service.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookup-types")
@RequiredArgsConstructor
public class LookupTypeController {
    private final LookupService lookupService;

    @GetMapping
    public ResponseEntity<List<LookupTypeSummaryResponse>> getAll() {
        return ResponseEntity.ok(lookupService.getAllLookupTypes());
    }

    @GetMapping("/{typeId}/values")
    public ResponseEntity<List<LookupValueResponse>> getValues(@PathVariable Long typeId) {
        return ResponseEntity.ok(lookupService.getLookupValues(typeId));
    }

    @PostMapping("/{typeId}/values")
    public ResponseEntity<LookupValueMutationResponse> createValue(
            @PathVariable Long typeId,
            @Valid @RequestBody LookupValueUpsertRequest request
    ) {
        return ResponseEntity.status(201).body(lookupService.createLookupValue(typeId, request));
    }

    @PutMapping("/{typeId}/values")
    public ResponseEntity<LookupValueMutationResponse> updateValue(
            @PathVariable Long typeId,
            @Valid @RequestBody LookupValueUpsertRequest request
    ) {
        return ResponseEntity.ok(lookupService.updateLookupValue(typeId, request));
    }
}
