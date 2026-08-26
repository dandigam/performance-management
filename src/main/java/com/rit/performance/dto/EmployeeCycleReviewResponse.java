package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCycleReviewResponse {
    private Long reviewId;
    private Long employeeId;
    private Long cycleId;
    private String cycleName;
    private LocalDate evaluationStartDate;
    private LocalDate evaluationEndDate;
    private String description;
    private Long reviewTypeId;
    private String reviewTypeName;
    private Long applicableTypeId;
    private List<Long> scopeValueIds;
    private String cycleStatus;
    private EmployeeReviewStatus reviewStatus;
    private EmployeeReviewStatus assessmentStatus;
    private Long assessorRoleId;
    private String assessorRoleName;
    private BigDecimal progressPercentage;
    private BigDecimal overallProgressPercentage;
    private LocalDate originalDueDate;
    private LocalDate dueDate;
    private boolean overdue;
    private boolean extended;
    private Integer extensionDaysPerStage;
    private String extensionReason;
    private LocalDateTime extendedAt;
    private LocalDateTime startedDate;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewCreatedDate;
    private LocalDateTime reviewUpdatedDate;
}
