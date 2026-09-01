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
    @NotBlank @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @NotBlank @Email @Size(max = 100)
    private String email;
    @Size(max = 20)
    private String phoneNumber;
    @Size(max = 30)
    private String gender;
    @Past
    private LocalDate dateOfBirth;
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
    private Long designationId;
    @jakarta.validation.Valid
    private ProjectAssignmentRequest projectAssignment;
    @jakarta.validation.Valid
    private EmployeeAddressRequest addressDetails;
    @jakarta.validation.Valid
    private EmployeeCompensationRequest compensationDetails;
    @jakarta.validation.Valid
    private EmployeeProfessionalDetailsRequest professionalDetails;
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid
            EmployeeEducationRequest> educationDetails;
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid
            EmployeeExperienceRequest> experienceDetails;
    @jakarta.validation.Valid
    private EmployeeBankDetailsRequest bankDetails;
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid EmployeeDocumentRequest> documentList;
    private Long createdBy;
}
