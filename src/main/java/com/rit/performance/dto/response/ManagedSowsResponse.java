package com.rit.performance.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ManagedSowsResponse(List<ManagedSow> managedSows) {
    public record ManagedSow(Long sowId, String sowName, String status,
                             LocalDate startDate, LocalDate endDate,
                             List<String> responsibilities, boolean canViewSow) {}
}
