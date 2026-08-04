package com.rit.performance.controller;

import com.rit.performance.entity.Projects;
import com.rit.performance.service.ProjectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}