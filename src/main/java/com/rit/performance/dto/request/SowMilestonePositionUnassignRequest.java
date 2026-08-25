package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowMilestonePositionUnassignRequest {
    @NotNull(message = "assignmentEndDate is required")
    private LocalDate assignmentEndDate;
    private Long updatedBy;
}
