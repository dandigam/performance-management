package com.rit.performance.controller;

import com.rit.performance.dto.ReportingManagerResponse;
import com.rit.performance.dto.EmployeeAssignmentRequest;
import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employee-assignments")
@RequiredArgsConstructor
public class EmployeeAssignmentController {
    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeBasicInfoResponse> assign(
            @Valid @RequestBody EmployeeAssignmentRequest request) {
        return ResponseEntity.ok(employeeService.assign(request));
    }

    @GetMapping("/reporting-managers")
    public ResponseEntity<List<ReportingManagerResponse>> getReportingManagers(
            @RequestParam(required = false) Long sowId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long excludeEmployeeId) {
        return ResponseEntity.ok(employeeService.getReportingManagers(
                sowId, departmentId, designationId, excludeEmployeeId));
    }
}
