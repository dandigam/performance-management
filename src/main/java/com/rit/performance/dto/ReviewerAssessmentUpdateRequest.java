package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerAssessmentUpdateRequest {
    @NotNull(message = "status is required")
    private EmployeeReviewStatus status;

    @NotNull(message = "overallRating is required")
    @DecimalMin(value = "1.0", message = "overallRating must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "overallRating must be between 1 and 5")
    private BigDecimal overallRating;

    private String overallComment;
    private Long updatedBy;
}
