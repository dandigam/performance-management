package com.rit.performance.dto;

public record ReportingTeamMemberResponse(
        Long employeeId,
        String employeeName,
        String ritId,
        String designationName,
        String status) {
}
