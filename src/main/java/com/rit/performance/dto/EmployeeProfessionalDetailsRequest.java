package com.rit.performance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProfessionalDetailsRequest {
    @Size(max = 10000)
    private String itSkills;

    @Size(max = 10000)
    private String latestExperience;
}
