package com.rit.performance.dto;

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
public class EmployeeExperienceResponse {
    private Long id;
    private String companyName;
    private String position;
    private String location;
    private LocalDate fromDate;
    private LocalDate endDate;
}
