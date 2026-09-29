package com.rit.performance.dto.report;

import java.util.List;
import java.util.Map;

public record EmployeeReportExportData(
        List<ReportResultColumn> columns,
        List<Map<String, Object>> content
) {}
