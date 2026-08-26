package com.rit.performance.controller;

import com.rit.performance.dto.CsxEmployeeResponse;
import com.rit.performance.dto.CsxEmployeeCreateRequest;
import com.rit.performance.dto.CsxEmployeeUpdateRequest;
import com.rit.performance.service.CsxEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/csx-employees")
@RequiredArgsConstructor
public class CsxEmployeeController {
    private final CsxEmployeeService service;

    @PostMapping
    public ResponseEntity<CsxEmployeeResponse> create(
            @Valid @RequestBody CsxEmployeeCreateRequest request
    ) {
        CsxEmployeeResponse created = service.create(request);
        return ResponseEntity.created(
                URI.create("/api/v1/csx-employees/" + created.getId())
        ).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CsxEmployeeResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CsxEmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CsxEmployeeUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CsxEmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
