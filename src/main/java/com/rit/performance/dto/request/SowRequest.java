package com.rit.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowRequest {
    @NotBlank(message = "sowCode is required")
    @Size(max = 50, message = "sowCode must not exceed 50 characters")
    private String sowCode;

    @NotBlank(message = "sowName is required")
    @Size(max = 200, message = "sowName must not exceed 200 characters")
    private String sowName;

    @NotNull(message = "businessUnitId is required")
    private Long businessUnitId;

    private LocalDate submittedDate;

    @Size(max = 100, message = "csxProjectId must not exceed 100 characters")
    private String csxProjectId;

    private Long csxContactEmployeeId;
    private Long csxEscalationEmployeeId;
    private Long ritContactEmployeeId;
    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    @Valid
    private List<SowMilestoneRequest> milestones;

    private List<@NotNull(message = "documentList cannot contain null values")
            @Valid SowDocumentRequest> documentList;
}
