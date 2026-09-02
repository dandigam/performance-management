package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionUnassignRequest {
    @NotNull(message = "assignmentEndDate is required")
    private LocalDate assignmentEndDate;

    @NotBlank(message = "assignmentStatus is required")
    @Pattern(regexp = "(?i)(COMPLETED|UNASSIGNED)",
            message = "assignmentStatus must be COMPLETED or UNASSIGNED")
    private String assignmentStatus;

    private Long updatedBy;
}
