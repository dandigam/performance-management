package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeEducationResponse {
    private Long id;
    private String educationType;
    private String collegeUniversity;
    private Integer passingYear;
    private BigDecimal percentage;
}
