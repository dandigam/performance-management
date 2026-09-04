package com.rit.performance.controller;

import com.rit.performance.dto.HolidayRequest;
import com.rit.performance.dto.HolidayResponse;
import com.rit.performance.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService service;

    @PostMapping
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        HolidayResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/holidays/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolidayResponse> update(
            @PathVariable Long id, @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.getAll(year, locationType, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
