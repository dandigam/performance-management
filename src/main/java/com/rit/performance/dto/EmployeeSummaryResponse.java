package com.rit.performance.dto;

import com.rit.performance.entity.WorkLocation;
import java.util.List;

public record EmployeeSummaryResponse(
        Long employeeId, String employeeName, String ritId, String email,
        Long designationId, String designationName, String employmentType,
        String workMode, WorkLocation workLocation, String status,
        Long departmentId, String departmentName, List<ProjectSummary> currentProjects) {
    public record ProjectSummary(Long sowId, String sowName) {}
}
