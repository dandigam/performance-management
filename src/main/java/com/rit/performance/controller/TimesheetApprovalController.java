package com.rit.performance.controller;

import com.rit.performance.dto.request.TimesheetApprovalRequest;
import com.rit.performance.dto.response.TimesheetApprovalUpdateResponse;
import com.rit.performance.service.impl.TimesheetApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timesheets")
public class TimesheetApprovalController {
    private final TimesheetApprovalService service;

    @PutMapping("/{timesheetId}/approvals/{approvalId}")
    public TimesheetApprovalUpdateResponse update(@PathVariable Long timesheetId,
            @PathVariable Long approvalId, @RequestBody TimesheetApprovalRequest request,
            Principal principal) {
        return service.update(timesheetId, approvalId, request,
                principal == null ? null : principal.getName());
    }
}
