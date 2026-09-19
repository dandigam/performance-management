package com.rit.performance.controller;

import com.rit.performance.dto.request.LeaveRequestDraftRequest;
import com.rit.performance.dto.response.*;
import com.rit.performance.service.MyLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my")
public class MyLeaveController {
    private final MyLeaveService service;

    @GetMapping("/leave-balances")
    public List<EmployeeLeaveBalanceResponse> balances(@RequestParam int year) {
        return service.myBalances(year);
    }

    @GetMapping("/leave-requests")
    public List<LeaveRequestResponse> requests() {
        return service.myRequests();
    }

    @GetMapping("/leave-requests/schedule")
    public LeaveScheduleResponse schedule(@RequestParam Long leaveTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.schedule(leaveTypeId, fromDate, toDate);
    }

    @GetMapping("/leave-requests/{id}")
    public LeaveRequestResponse request(@PathVariable Long id) {
        return service.myRequest(id);
    }

    @PostMapping("/leave-requests")
    public ResponseEntity<LeaveRequestResponse> createDraft(@Valid @RequestBody LeaveRequestDraftRequest request) {
        var result = service.createDraft(request);
        return ResponseEntity.created(URI.create("/api/v1/my/leave-requests/" + result.id())).body(result);
    }

    @PutMapping("/leave-requests/{id}")
    public LeaveRequestResponse updateDraft(@PathVariable Long id,
            @Valid @RequestBody LeaveRequestDraftRequest request) {
        return service.updateDraft(id, request);
    }

    @PostMapping("/leave-requests/{id}/submit")
    public LeaveRequestResponse submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/leave-requests/{id}/cancel")
    public LeaveRequestResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
