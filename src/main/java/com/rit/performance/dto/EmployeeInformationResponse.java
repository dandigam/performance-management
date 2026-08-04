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
public class EmployeeInformationResponse {
    private Long employeeId;
    private String employeeName;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String ritId;
    private String csxRacfId;
    private LocalDate joiningDate;
    private String employmentType;
    private String status;
}
