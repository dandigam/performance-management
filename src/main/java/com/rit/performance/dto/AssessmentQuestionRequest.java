package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentQuestionRequest {

    private Long id;

    @NotBlank(message = "questionText is required")
    private String questionText;

    @NotBlank(message = "questionType is required")
    private String questionType;

    @NotNull(message = "required is required")
    private Boolean required;

    @NotNull(message = "allowComments is required")
    private Boolean allowComments;

    @NotNull(message = "displayOrder is required")
    @Positive(message = "displayOrder must be greater than zero")
    private Integer displayOrder;
}
