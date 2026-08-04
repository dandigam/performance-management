package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAssessmentRequest {
    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "cycleId is required")
    private Long cycleId;

    private Long assessmentId;

    private Long assessorEmployeeId;

    @NotNull(message = "status is required")
    private EmployeeReviewStatus status;

    private List<@Valid EmployeeReviewAnswerRequest> answers;

    private java.math.BigDecimal overallRating;
    private String overallComment;

    private Long updatedBy;
}
