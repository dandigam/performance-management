package com.rit.performance.dto.report;

import jakarta.validation.constraints.NotBlank;

public record ReportSortRequest(@NotBlank String field, @NotBlank String direction) {}
