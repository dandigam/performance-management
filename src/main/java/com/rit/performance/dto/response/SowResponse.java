package com.rit.performance.dto.response;

import com.rit.performance.dto.DocumentResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowResponse {
    private Long id;
    private String sowCode;
    private String sowName;
    private Long businessUnitId;
    private String businessUnitName;
    private LocalDate submittedDate;
    private String csxProjectId;
    private Long csxContactEmployeeId;
    private String csxContactEmployeeName;
    private String csxContactEmployeeEmail;
    private Long csxEscalationEmployeeId;
    private String csxEscalationEmployeeName;
    private String csxEscalationEmployeeEmail;
    private Long ritContactEmployeeId;
    private String ritContactEmployeeName;
    private String ritContactEmployeeEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<SowMilestoneResponse> milestones;
    private List<DocumentResponse> documentList;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
