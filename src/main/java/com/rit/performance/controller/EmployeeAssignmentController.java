package com.rit.performance.controller;

import com.rit.performance.dto.ReportingManagerResponse;
import com.rit.performance.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employee-assignments")
@RequiredArgsConstructor
public class EmployeeAssignmentController {
    private final EmployeeService employeeService;

    @GetMapping("/reporting-managers")
    public ResponseEntity<List<ReportingManagerResponse>> getReportingManagers(
            @RequestParam Long projectId,
            @RequestParam Long departmentId,
            @RequestParam(defaultValue = "21") Long designationId,
            @RequestParam(required = false) Long excludeEmployeeId) {
        return ResponseEntity.ok(employeeService.getReportingManagers(
                projectId, departmentId, designationId, excludeEmployeeId));
    }
}
