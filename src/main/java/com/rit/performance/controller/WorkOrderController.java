package com.rit.performance.controller;

import com.rit.performance.dto.request.WorkOrderRequest;
import com.rit.performance.dto.response.WorkOrderResponse;
import com.rit.performance.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrderResponse created = workOrderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/work-orders/" + created.getId()))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponse>> getAll() {
        return ResponseEntity.ok(workOrderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> update(
            @PathVariable Long id, @Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
