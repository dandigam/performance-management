package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import com.rit.performance.dto.response.SowMilestonePositionAssignmentResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeBasicInfoResponse {
    private Long employeeId;
    private String employeeName;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String ritId;
    private String csxRacfId;
    private String employmentType;
    private LocalDate joiningDate;
    private String workMode;
    private Long vendorId;
    private String vendorCompanyName;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long designationId;
    private String designationName;
    @JsonIgnore
    private Long assignmentId;
    @JsonIgnore
    private Long sowId;
    @JsonIgnore
    private String sowCode;
    @JsonIgnore
    private String sowName;
    @JsonIgnore
    private Long milestoneId;
    @JsonIgnore
    private String milestoneName;
    @JsonIgnore
    private String positionType;
    @JsonIgnore
    private Boolean isPrimaryAssignment;
    @JsonIgnore
    private Integer allocationPercentage;
    @JsonIgnore
    private LocalDate assignmentStartDate;
    @JsonIgnore
    private LocalDate assignmentEndDate;
    @JsonIgnore
    private String assignmentStatus;
    @JsonIgnore
    private Long departmentId;
    @JsonIgnore
    private String departmentName;
    @JsonIgnore
    private Long managerId;
    @JsonIgnore
    private String managerName;
    @JsonIgnore
    private Long leadId;
    @JsonIgnore
    private String leadName;
    @JsonIgnore
    private List<EmployeeAssignmentResponse> assignmentList;
    private List<EmployeeCurrentProjectResponse> currentProjects;
    private List<SowMilestonePositionAssignmentResponse> milestoneAssignments;
    private String status;
    private EmployeeAddressResponse addressDetails;
    private EmployeeCompensationResponse compensationDetails;
    private EmployeeProfessionalDetailsResponse professionalDetails;
    private EmployeeBankDetailsResponse bankDetails;
    private List<DocumentResponse> documentList;
    private EmployeeReviewSummaryResponse review;
}
