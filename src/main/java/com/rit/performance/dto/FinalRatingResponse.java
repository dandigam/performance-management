package com.rit.performance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalRatingResponse {
    private Long id;
    private Long employeeReviewId;
    private Long employeeId;
    private String employeeName;
    private Long cycleId;
    private String cycleName;
    private BigDecimal finalRating;
    private Boolean published;
    private LocalDateTime publishedDate;
    private Long publishedById;
    private String publishedByUsername;
}
