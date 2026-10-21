package com.rit.performance.dto.response;

import java.util.List;

public record SowMilestoneSummaryPageResponse(
        List<SowMilestoneSummaryResponse> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
}
