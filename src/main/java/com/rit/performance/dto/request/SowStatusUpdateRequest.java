package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "statusEffectiveDate is required")
    private LocalDate statusEffectiveDate;
}
