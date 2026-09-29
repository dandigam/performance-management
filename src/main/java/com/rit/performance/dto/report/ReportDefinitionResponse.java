package com.rit.performance.dto.report;

import java.util.List;

public record ReportDefinitionResponse(String reportCode, List<ReportColumnDefinition> columns) {}
