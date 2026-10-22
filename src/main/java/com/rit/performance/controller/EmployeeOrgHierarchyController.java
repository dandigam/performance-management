package com.rit.performance.controller;

import com.rit.performance.dto.request.EmployeeOrgHierarchyRequest;
import com.rit.performance.dto.request.EmployeeOrgHierarchyBulkRequest;
import com.rit.performance.dto.response.EmployeeOrgHierarchyResponse;
import com.rit.performance.service.EmployeeOrgHierarchyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-org-hierarchy")
@RequiredArgsConstructor
public class EmployeeOrgHierarchyController {
    private final EmployeeOrgHierarchyService service;
    private final com.rit.performance.service.EmployeeOrgHierarchyExportService exportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        byte[] workbook = exportService.export();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employee-organization-hierarchy.xlsx\"")
                .body(workbook);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<EmployeeOrgHierarchyResponse>> createBulk(
            @Valid @RequestBody EmployeeOrgHierarchyBulkRequest request) {
        return ResponseEntity.status(201).body(service.createBulk(request));
    }

    @PostMapping
    public ResponseEntity<EmployeeOrgHierarchyResponse> create(@Valid @RequestBody EmployeeOrgHierarchyRequest request) {
        var response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employee-org-hierarchy/" + response.id())).body(response);
    }

    @GetMapping
    public List<EmployeeOrgHierarchyResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public EmployeeOrgHierarchyResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/employee/{employeeId}")
    public EmployeeOrgHierarchyResponse getByEmployee(@PathVariable Long employeeId) {
        return service.getByEmployee(employeeId);
    }

    @PutMapping("/{id}")
    public EmployeeOrgHierarchyResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeOrgHierarchyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
