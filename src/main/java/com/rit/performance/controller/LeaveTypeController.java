package com.rit.performance.controller;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.LeaveTypeResponse;
import com.rit.performance.entity.LeaveTypeStatus;
import com.rit.performance.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/leave-types")
public class LeaveTypeController {
    private final LeaveTypeService service;

    @PostMapping
    public ResponseEntity<LeaveTypeResponse> create(@Valid @RequestBody LeaveTypeRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/leave-types/" + result.id())).body(result);
    }

    @GetMapping
    public List<LeaveTypeResponse> getAll(@RequestParam(required = false) LeaveTypeStatus status) {
        return service.getAll(status);
    }

    @GetMapping("/{id}")
    public LeaveTypeResponse getById(@PathVariable Long id) { return service.getById(id); }

    @PutMapping("/{id}")
    public LeaveTypeResponse update(@PathVariable Long id, @Valid @RequestBody LeaveTypeRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public LeaveTypeResponse changeStatus(@PathVariable Long id, @Valid @RequestBody LeaveTypeStatusRequest request) {
        return service.changeStatus(id, request.status());
    }
}
