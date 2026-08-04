package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleDetailsRequest {

    private Long id;

    @Pattern(regexp = "DRAFT|PUBLISHED|ACTIVE|COMPLETED|ARCHIVED",
            message = "status must be DRAFT, PUBLISHED, ACTIVE, COMPLETED, or ARCHIVED")
    private String status;

    @NotBlank(message = "cycleName is required")
    private String cycleName;

    private LocalDate evaluationStartDate;

    private LocalDate evaluationEndDate;

    private String description;

    @NotNull(message = "reviewTypeId is required")
    private Long reviewTypeId;

    @NotNull(message = "applicableTypeId is required")
    private Long applicableTypeId;

    private List<Long> scopeValueIds;
}
