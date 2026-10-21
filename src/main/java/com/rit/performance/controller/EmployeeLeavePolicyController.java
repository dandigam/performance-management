package com.rit.performance.controller;

import com.rit.performance.dto.request.EmployeeLeavePolicyRequest;
import com.rit.performance.dto.request.LeavePolicyStatusRequest;
import com.rit.performance.dto.response.EmployeeLeavePolicyResponse;
import com.rit.performance.service.EmployeeLeavePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees/{employeeId}/leave-policies")
public class EmployeeLeavePolicyController {
    private final EmployeeLeavePolicyService service;

    @PostMapping
    public ResponseEntity<EmployeeLeavePolicyResponse> assign(@PathVariable Long employeeId,
            @Valid @RequestBody EmployeeLeavePolicyRequest request) {
        var result = service.assign(employeeId, request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employeeId + "/leave-policies/" + result.id()))
                .body(result);
    }

    @PutMapping("/{assignmentId}")
    public EmployeeLeavePolicyResponse update(@PathVariable Long employeeId, @PathVariable Long assignmentId,
            @Valid @RequestBody EmployeeLeavePolicyRequest request) {
        return service.update(employeeId, assignmentId, request);
    }

    @GetMapping
    public List<EmployeeLeavePolicyResponse> getForEmployee(@PathVariable Long employeeId) {
        return service.getForEmployee(employeeId);
    }

    @GetMapping("/current")
    public EmployeeLeavePolicyResponse getCurrent(@PathVariable Long employeeId) {
        return service.getCurrent(employeeId);
    }

    @PatchMapping("/{assignmentId}/status")
    public EmployeeLeavePolicyResponse changeStatus(@PathVariable Long employeeId, @PathVariable Long assignmentId,
            @Valid @RequestBody LeavePolicyStatusRequest request) {
        return service.changeStatus(employeeId, assignmentId, request.status());
    }
}
