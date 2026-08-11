package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeCreateRequest {
    @NotBlank @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @NotBlank @Email @Size(max = 100)
    private String email;
    @Size(max = 20)
    private String phoneNumber;
    @JsonAlias("ritEmployeeId")
    @Size(max = 50)
    private String ritId;
    @Size(max = 50)
    private String csxRacfId;
    @NotBlank @Size(max = 50)
    private String employmentType;
    @NotBlank @Size(max = 50)
    private String workMode;
    @PastOrPresent
    private LocalDate joiningDate;
    @Positive
    private Long vendorId;
    @Size(max = 20)
    private String status;
    @Positive
    private Long roleId;
    @Positive
    private Long departmentId;
    @Positive
    private Long designationId;
    @Positive
    private Long managerId;
    @Positive
    private Long leadId;
    @Positive
    private Long projectId;
    @PastOrPresent
    private LocalDate assignmentEffectiveFrom;
    @jakarta.validation.Valid
    private ProjectAssignmentRequest projectAssignment;
    private Long createdBy;
}
