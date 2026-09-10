package com.rit.performance.controller;

import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetEmployeeProjectResponse;
import com.rit.performance.dto.response.TimesheetEmployeeProjectSummaryResponse;
import com.rit.performance.service.TimesheetEmployeeProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/timesheet-projects")
@RequiredArgsConstructor
public class TimesheetEmployeeProjectController {
    private final TimesheetEmployeeProjectService service;

    @PostMapping
    public ResponseEntity<List<TimesheetEmployeeProjectResponse>> create(
            @PathVariable Long employeeId,
            @Valid @RequestBody List<@Valid TimesheetEmployeeProjectRequest> requests) {
        List<TimesheetEmployeeProjectResponse> saved = service.create(employeeId, requests);
        return ResponseEntity.ok(saved);
    }

    @PutMapping
    public ResponseEntity<List<TimesheetEmployeeProjectResponse>> update(
            @PathVariable Long employeeId,
            @Valid @RequestBody List<@Valid TimesheetEmployeeProjectRequest> requests) {
        return ResponseEntity.ok(service.update(employeeId, requests));
    }

    @GetMapping
    public ResponseEntity<List<TimesheetEmployeeProjectSummaryResponse>> getAll(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(service.getAll(employeeId));
    }

    @GetMapping("/{sowId}/milestones/{milestoneId}")
    public ResponseEntity<TimesheetEmployeeProjectResponse> get(
            @PathVariable Long employeeId,
            @PathVariable Long sowId,
            @PathVariable Long milestoneId) {
        return ResponseEntity.ok(service.get(employeeId, sowId, milestoneId));
    }
}
