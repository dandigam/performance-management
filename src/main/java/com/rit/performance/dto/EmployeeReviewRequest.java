package com.rit.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReviewRequest {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "cycleId is required")
    private Long cycleId;

    @DecimalMin(value = "1.0", message = "selfRating must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "selfRating must be between 1 and 5")
    private BigDecimal selfRating;

    private String overallComment;

    private BigDecimal progressPercentage;
    private LocalDateTime startedDate;
    private Long createdBy;
    private Long updatedBy;
    private Integer managerId;
    private Integer managerSubmittedDate;
    private Integer hrId;
    private LocalDateTime hrSubmittedDate;
    private Integer finalRating;

    private List<EmployeeReviewAnswerRequest> answers;
}
