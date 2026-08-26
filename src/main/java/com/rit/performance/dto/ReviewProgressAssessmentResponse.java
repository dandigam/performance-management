package com.rit.performance.dto;

import com.rit.performance.service.EmployeeReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewProgressAssessmentResponse {
    private Long stageId;
    private String roleName;
    private EmployeeReviewStatus status;
    private LocalDate dueDate;
    private boolean overdue;
}
