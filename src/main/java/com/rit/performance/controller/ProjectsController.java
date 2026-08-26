package com.rit.performance.controller;

import com.rit.performance.entity.Projects;
import com.rit.performance.service.ProjectsService;
import com.rit.performance.dto.ProjectEmployeesResponse;
import com.rit.performance.dto.ProjectEmployeeCreateRequest;
import com.rit.performance.dto.ProjectEmployeeResponse;
import com.rit.performance.dto.ProjectEmployeeStatusUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.net.URI;

@RestController
@RequestMapping({"/api/projects", "/api/v1/projects"})
@RequiredArgsConstructor
@Validated
public class ProjectsController {

    private final ProjectsService projectService;

    @PostMapping
    public ResponseEntity<Projects> create(@RequestBody Projects project) {
        return ResponseEntity.ok(projectService.save(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Projects> update(@PathVariable Long id,
                                           @RequestBody Projects project) {
        return ResponseEntity.ok(projectService.update(id, project));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Projects> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Projects>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @GetMapping("/{projectId}/employees")
    public ResponseEntity<ProjectEmployeesResponse> getEmployees(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(projectService.getEmployees(projectId, page, size));
    }

    @PostMapping("/{projectId}/employees")
    public ResponseEntity<ProjectEmployeeResponse> addEmployee(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectEmployeeCreateRequest request) {
        ProjectEmployeeResponse created = projectService.addEmployee(projectId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/projects/" + projectId + "/employees/" + created.getAssignmentId()))
                .body(created);
    }

    @PatchMapping("/{projectId}/employees/{assignmentId}/status")
    public ResponseEntity<ProjectEmployeeResponse> updateEmployeeAssignmentStatus(
            @PathVariable Long projectId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody ProjectEmployeeStatusUpdateRequest request) {
        return ResponseEntity.ok(
                projectService.updateEmployeeAssignmentStatus(projectId, assignmentId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
