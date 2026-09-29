package com.rit.performance.dto.report;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record ReportFilterRequest(
        @NotBlank String field,
        @NotBlank String operator,
        JsonNode value
) {}
