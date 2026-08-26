package com.rit.performance.controller;

import com.rit.performance.dto.RateCardRequest;
import com.rit.performance.dto.RateCardResponse;
import com.rit.performance.service.RateCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rate-cards")
@RequiredArgsConstructor
public class RateCardController {
    private final RateCardService service;
    @PostMapping public ResponseEntity<RateCardResponse> create(@Valid @RequestBody RateCardRequest request) {
        RateCardResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/rate-cards/" + created.getId())).body(created);
    }
    @PutMapping("/{id}") public ResponseEntity<RateCardResponse> update(@PathVariable Long id, @Valid @RequestBody RateCardRequest request) { return ResponseEntity.ok(service.update(id, request)); }
    @GetMapping("/{id}") public ResponseEntity<RateCardResponse> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }
    @GetMapping public ResponseEntity<List<RateCardResponse>> getAll() { return ResponseEntity.ok(service.getAll()); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
