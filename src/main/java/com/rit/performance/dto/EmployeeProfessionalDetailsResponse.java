package com.rit.performance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfessionalDetailsResponse {
    private Long id;
    private String itSkills;
    private String latestExperience;
}
