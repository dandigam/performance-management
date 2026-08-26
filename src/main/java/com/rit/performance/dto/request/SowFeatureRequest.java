package com.rit.performance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowFeatureRequest {
    @NotNull(message = "milestoneId is required")
    private Long milestoneId;

    @NotBlank(message = "featureName is required")
    @Size(max = 200, message = "featureName must not exceed 200 characters")
    private String featureName;

    @Size(max = 1000, message = "description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

}
