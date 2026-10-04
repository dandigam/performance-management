package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SowOwnersUpdateRequest(Long deliveryOwnerEmployeeId, Long technicalLeadEmployeeId,
        @NotNull @PastOrPresent LocalDate effectiveDate, @Size(max = 2000) String reason) {}
