package com.rit.performance.controller;

import com.rit.performance.dto.EmployeeHierarchyImportResponse;
import com.rit.performance.service.EmployeeHierarchyImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/employee-org-hierarchy/import")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class EmployeeHierarchyImportController {
    private final EmployeeHierarchyImportService service;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        return download(service.template(), "employee-hierarchy-template.xlsx");
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeHierarchyImportResponse validate(@RequestParam("file") MultipartFile file,
                                                     Principal principal) throws IOException {
        return service.validate(file, principal.getName());
    }

    @PostMapping("/{importId}/commit")
    public EmployeeHierarchyImportResponse commit(@PathVariable String importId) throws IOException {
        return service.commit(importId);
    }

    @GetMapping("/{importId}")
    public EmployeeHierarchyImportResponse status(@PathVariable String importId) throws IOException {
        return service.status(importId);
    }

    @GetMapping("/{importId}/errors")
    public ResponseEntity<byte[]> errors(@PathVariable String importId) throws IOException {
        return download(service.errors(importId), "employee-hierarchy-errors.xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String name) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(bytes);
    }
}
