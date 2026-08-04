package com.rit.performance.dto;

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
public class AssessmentQuestionResponse {

    private Long id;

    private String questionText;

    private String questionType;

    private Boolean required;

    private Boolean allowComments;

    private Integer displayOrder;
}
