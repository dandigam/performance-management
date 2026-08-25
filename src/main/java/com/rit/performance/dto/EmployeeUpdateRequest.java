package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EmployeeUpdateRequest {
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @Email
    @Size(max = 100)
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
    @Size(max = 50)
    private String employmentType;
    @Size(max = 50)
    private String workMode;
    @PastOrPresent
    private LocalDate joiningDate;
    @Positive
    private Long vendorId;
    @JsonIgnore
    private boolean vendorIdPresent;
    @Size(max = 20)
    private String status;

    @Positive
    private Long roleId;
    @JsonIgnore
    private boolean roleIdPresent;
    @Positive
    private Long designationId;
    @JsonIgnore
    private boolean designationIdPresent;
    @jakarta.validation.Valid
    private ProjectAssignmentRequest projectAssignment;
    @jakarta.validation.Valid
    private EmployeeAddressRequest addressDetails;
    @jakarta.validation.Valid
    private EmployeeCompensationRequest compensationDetails;
    @jakarta.validation.Valid
    private EmployeeProfessionalDetailsRequest professionalDetails;
    @jakarta.validation.Valid
    private EmployeeBankDetailsRequest bankDetails;
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid EmployeeDocumentRequest> documentList;
    private Long updatedBy;

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
        this.roleIdPresent = true;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
        this.designationIdPresent = true;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
        this.vendorIdPresent = true;
    }
}
