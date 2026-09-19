package com.rit.performance.controller;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.*;
import com.rit.performance.service.EmployeeLeaveBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees/{employeeId}/leave-balances")
public class EmployeeLeaveBalanceController {
    private final EmployeeLeaveBalanceService service;

    @PostMapping("/initialize")
    public List<EmployeeLeaveBalanceResponse> initialize(@PathVariable Long employeeId,
            @Valid @RequestBody InitializeLeaveBalancesRequest request) {
        return service.initialize(employeeId, request);
    }

    @GetMapping
    public List<EmployeeLeaveBalanceResponse> getForEmployee(@PathVariable Long employeeId,
            @RequestParam int year) {
        return service.getForEmployee(employeeId, year);
    }

    @GetMapping("/{balanceId}")
    public EmployeeLeaveBalanceResponse getById(@PathVariable Long employeeId, @PathVariable Long balanceId) {
        return service.getById(employeeId, balanceId);
    }

    @PostMapping("/{balanceId}/adjustments")
    public ResponseEntity<LeaveBalanceAdjustmentResponse> addAdjustment(@PathVariable Long employeeId,
            @PathVariable Long balanceId, @Valid @RequestBody LeaveBalanceAdjustmentRequest request) {
        var result = service.addAdjustment(employeeId, balanceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{balanceId}/adjustments")
    public List<LeaveBalanceAdjustmentResponse> getAdjustments(@PathVariable Long employeeId,
            @PathVariable Long balanceId) {
        return service.getAdjustments(employeeId, balanceId);
    }
}
