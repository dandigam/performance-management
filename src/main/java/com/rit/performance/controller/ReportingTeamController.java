package com.rit.performance.controller;

import com.rit.performance.dto.ReportingTeamResponse;
import com.rit.performance.service.ReportingTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportingTeamController {
    private final ReportingTeamService service;

    @GetMapping("/api/employees/me/reporting-team")
    public ReportingTeamResponse getMyReportingTeam() {
        return service.getMyReportingTeam();
    }
}
