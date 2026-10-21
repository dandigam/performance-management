package com.rit.performance.dto.report;

import java.util.List;

public record EmployeeWorkforceReportResponse(
        EmployeeWorkforceReportSummary summary,
        List<EmployeeWorkforceReportRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
