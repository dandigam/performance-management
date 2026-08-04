package com.rit.performance.controller;

import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.dto.EmployeeCreateRequest;
import com.rit.performance.dto.EmployeeCreateResponse;
import com.rit.performance.dto.DirectReportsResponse;
import com.rit.performance.dto.EmployeeUpdateRequest;
import com.rit.performance.dto.EmployeeHierarchyResponse;
import com.rit.performance.dto.EmployeeInformationResponse;
import com.rit.performance.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeCreateResponse> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeBasicInfoResponse>> getBasicInfo() {
        return ResponseEntity.ok(employeeService.getBasicInfo());
    }

    @GetMapping("/information")
    public ResponseEntity<List<EmployeeInformationResponse>> getEmployeeInformation() {
        return ResponseEntity.ok(employeeService.getEmployeeInformation());
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<EmployeeHierarchyResponse> getHierarchy(
            @RequestParam Long employeeId,
            @RequestParam String roleType,
            @RequestParam Long cycleId) {
        return ResponseEntity.ok(employeeService.getHierarchy(employeeId, roleType, cycleId));
    }

    @GetMapping("/{teamLeadEmployeeId}/direct-reports")
    public ResponseEntity<DirectReportsResponse> getDirectReports(@PathVariable Long teamLeadEmployeeId) {
        return ResponseEntity.ok(employeeService.getDirectReports(teamLeadEmployeeId));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeBasicInfoResponse> update(@PathVariable Long employeeId,
                                                            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(employeeId, request));
    }
}
