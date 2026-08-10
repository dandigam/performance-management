package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEmployeeResponse {
    private Long assignmentId;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String email;
    private Long designationId;
    private String designationName;
    private Long departmentId;
    private String departmentName;
    private String workMode;
    private Integer allocationPercentage;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String status;
}
