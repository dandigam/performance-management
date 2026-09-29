package com.rit.performance.dto.report;

import jakarta.validation.Valid;

import java.util.List;

public record ReportExportRequest(
        List<String> columns,
        List<@Valid ReportFilterRequest> filters,
        List<@Valid ReportSortRequest> sort
) {}
