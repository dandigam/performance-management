package com.rit.performance.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportColumnDefinition(
        String key,
        String label,
        String type,
        String lookupCode,
        boolean selectable,
        boolean defaultVisible,
        boolean sortable,
        boolean filterable,
        List<String> operators,
        List<ReportOption> options
) {}
