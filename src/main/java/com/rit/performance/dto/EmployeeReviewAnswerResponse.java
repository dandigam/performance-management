package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReviewAnswerResponse {
    private Long id;
    private Long sectionId;
    private String sectionName;
    private Long questionId;
    private String questionText;
    private String responseType;
    private Boolean required;
    private Integer rating;
    private String comment;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
