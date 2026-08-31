package com.rit.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Size(max = 50, message = "sowCode must not exceed 50 characters")
    private String sowCode;

    @NotBlank(message = "sowName is required")
    @Size(max = 200, message = "sowName must not exceed 200 characters")
    private String sowName;

    @NotNull(message = "year is required")
    @Min(value = 1900, message = "year must be 1900 or later")
    @Max(value = 9999, message = "year must be a four-digit year")
    private Integer year;

    @NotNull(message = "clientId is required")
    private Long clientId;

    @NotBlank(message = "sowType is required")
    @Size(max = 100, message = "sowType must not exceed 100 characters")
    private String sowType;

    @NotBlank(message = "engagementType is required")
    @Size(max = 100, message = "engagementType must not exceed 100 characters")
    private String engagementType;

    @NotNull(message = "businessUnitId is required")
    private Long businessUnitId;

    private LocalDate submittedDate;

    @Size(max = 100, message = "csxProjectId must not exceed 100 characters")
    private String csxProjectId;

    private Long projectOwnerEmployeeId;
    private Long csxContactEmployeeId;
    private Long csxEscalationEmployeeId;
    private Long ritContactEmployeeId;
    private Long ritEscalationEmployeeId;
    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    @Size(max = 2000, message = "remarks must not exceed 2000 characters")
    private String remarks;

    @Size(max = 20, message = "signedStatus must not exceed 20 characters")
    private String signedStatus;

    private LocalDate signedDate;

    private List<@Valid SowMilestoneRequest> milestones;

    private List<@NotNull(message = "documentList cannot contain null values")
            @Valid SowDocumentRequest> documentList;
}
