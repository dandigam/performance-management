package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class AssessmentSectionRequest {

    private Long id;

    @NotBlank(message = "sectionName is required")
    private String sectionName;

    @NotNull(message = "displayOrder is required")
    @Positive(message = "displayOrder must be greater than zero")
    private Integer displayOrder;

    @NotEmpty(message = "questions must contain at least one question")
    private List<@Valid AssessmentQuestionRequest> questions;
}
