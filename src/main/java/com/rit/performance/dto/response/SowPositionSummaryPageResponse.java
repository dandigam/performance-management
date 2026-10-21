package com.rit.performance.dto.response;

import java.util.List;

public record SowPositionSummaryPageResponse(
        List<SowPositionSummaryResponse> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
}
