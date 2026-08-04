package com.rit.performance.controller;

import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.service.SowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/sows")
@RequiredArgsConstructor
public class SowController {
    private final SowService sowService;

    @PostMapping
    public ResponseEntity<SowResponse> create(@Valid @RequestBody SowRequest request) {
        SowResponse created = sowService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sows/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<SowResponse>> getAll() {
        return ResponseEntity.ok(sowService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SowResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sowService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SowResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody SowRequest request) {
        return ResponseEntity.ok(sowService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
