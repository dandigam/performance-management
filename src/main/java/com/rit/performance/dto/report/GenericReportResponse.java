package com.rit.performance.dto.report;

import java.util.List;
import java.util.Map;

public record GenericReportResponse(
        Map<String, Long> summary,
        List<ReportResultColumn> columns,
        List<Map<String, Object>> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
