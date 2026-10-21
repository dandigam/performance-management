package com.rit.performance.dto.report;

import java.time.LocalDate;

public record EmployeeWorkforceReportRow(
        Long employeeId,
        String employeeNumber,
        String employeeName,
        String email,
        String departmentName,
        String designationName,
        String employmentType,
        String workMode,
        String workLocation,
        String status,
        LocalDate joiningDate
) {}
