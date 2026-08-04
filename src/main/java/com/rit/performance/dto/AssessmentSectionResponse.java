package com.rit.performance.dto;

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
public class AssessmentSectionResponse {

    private Long id;

    private String sectionName;

    private Integer displayOrder;

    private List<AssessmentQuestionResponse> questions;
}
