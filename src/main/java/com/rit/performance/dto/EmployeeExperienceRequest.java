package com.rit.performance.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeExperienceRequest {
    private String companyName;

    private String position;

    private String location;

    private LocalDate fromDate;

    private LocalDate endDate;
}
