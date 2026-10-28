package com.rit.performance.dto;

import java.util.List;

public record ReportingTeamResponse(
        Long managerEmployeeId,
        String managerEmployeeName,
        int totalReports,
        List<ReportingTeamMemberResponse> employees) {
}
