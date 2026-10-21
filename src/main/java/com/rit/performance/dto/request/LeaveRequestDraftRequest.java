package com.rit.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record LeaveRequestDraftRequest(
        @NotNull Long leaveTypeId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 2000) String notes,
        @NotEmpty List<@Valid LeaveRequestDayRequest> days) {}
