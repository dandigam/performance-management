package com.rit.performance.controller;

import com.rit.performance.dto.request.LeaveApprovalRequest;
import com.rit.performance.dto.request.LeaveRejectionRequest;
import com.rit.performance.dto.response.TeamLeaveDetailResponse;
import com.rit.performance.dto.response.TeamLeaveRequestResponse;
import com.rit.performance.service.LeaveApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leave-approvals")
public class LeaveApprovalController {
    private final LeaveApprovalService service;

    @GetMapping("/my-team/pending")
    public List<TeamLeaveRequestResponse> pending() { return service.team("pending"); }
    @GetMapping("/my-team/approved")
    public List<TeamLeaveRequestResponse> approved() { return service.team("approved"); }
    @GetMapping("/my-team/rejected")
    public List<TeamLeaveRequestResponse> rejected() { return service.team("rejected"); }
    @GetMapping("/my-team/all")
    public List<TeamLeaveRequestResponse> all() { return service.team("all"); }
    @GetMapping("/{requestId}")
    public TeamLeaveDetailResponse detail(@PathVariable Long requestId) { return service.detail(requestId); }
    @PostMapping("/{requestId}/approve")
    public TeamLeaveDetailResponse approve(@PathVariable Long requestId,
            @RequestBody(required = false) LeaveApprovalRequest request) {
        return service.approve(requestId, request == null ? null : request.comments());
    }
    @PostMapping("/{requestId}/reject")
    public TeamLeaveDetailResponse reject(@PathVariable Long requestId,
            @Valid @RequestBody LeaveRejectionRequest request) {
        return service.reject(requestId, request.reason());
    }
}
