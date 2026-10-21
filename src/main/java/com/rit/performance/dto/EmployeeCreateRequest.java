package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EmployeeCreateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    @JsonAlias("ritEmployeeId")
    private String ritEmployeeId;
    private String csxRacfId;
    private String employmentType;
    private String workMode;
    private String workLocation;
    private LocalDate joiningDate;
    private Long vendorId;
    private String status;
    private Long roleId;
    private Long designationId;
    private ProjectAssignmentRequest projectAssignment;
    private EmployeeAddressRequest addressDetails;
    private EmployeeCompensationRequest compensationDetails;
    private EmployeeProfessionalDetailsRequest professionalDetails;
    private  List<EmployeeEducationRequest> educationDetails;
    private List<EmployeeExperienceRequest> experienceDetails;
    private EmployeeBankDetailsRequest bankDetails;
    private List<EmployeeDocumentRequest> documentList;
    private Long createdBy;
}
