package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String gender;
    private java.time.LocalDate dateOfBirth;
    private String ritId;
    private String csxRacfId;
    private String employmentType;
    private java.time.LocalDate joiningDate;
    private String workMode;
    private Long vendorId;
    private String vendorCompanyName;
    private Long designationId;
    private String designationName;
    private String status;
    private EmployeeAddressResponse addressDetails;
    private EmployeeCompensationResponse compensationDetails;
    private EmployeeProfessionalDetailsResponse professionalDetails;
    private EmployeeBankDetailsResponse bankDetails;
    private java.util.List<DocumentResponse> documentList;
}
