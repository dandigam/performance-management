package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReviewAnswerRequest {

    @NotNull(message = "sectionId is required")
    private Long sectionId;

    @NotNull(message = "questionId is required")
    private Long questionId;

    @JsonAlias("selfRating")
    private Integer rating;
    private String comment;
    private Long createdBy;
    private Long updatedBy;
}
