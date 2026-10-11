package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowStatusUpdateRequest {
    @NotBlank(message = "status is required")
    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    // Selected work start date on first ACTIVE transition; future dates are allowed.
    // The persisted status effective date is always the backend business date.
    private LocalDate statusEffectiveDate;

    @Size(max = 2000, message = "reason must not exceed 2000 characters")
    private String reason;
}
