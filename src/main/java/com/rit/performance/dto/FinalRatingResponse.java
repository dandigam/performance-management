package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FinalRatingResponse> performance;
}
