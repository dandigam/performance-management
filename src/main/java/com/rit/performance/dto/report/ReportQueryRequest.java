package com.rit.performance.dto.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record ReportQueryRequest(
        List<String> columns,
        List<@Valid ReportFilterRequest> filters,
        List<@Valid ReportSortRequest> sort,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {}
