package com.rit.performance.dto;

import java.time.LocalDate;
import java.util.List;

/** Explicit allowlist of fields returned by the authenticated self-profile API. */
public record EmployeeSelfProfileResponse(
        Long employeeId, String employeeName, String firstName, String lastName,
        String email, String phoneNumber, String gender, LocalDate dateOfBirth,
        String ritId, String csxRacfId, String employmentType, LocalDate joiningDate,
        String workMode, String workLocation, String status,
        Long roleId, String roleCode, String roleName,
        Long designationId, String designationName,
        List<EmployeeCurrentProjectResponse> currentProjects,
        EmployeeAddressResponse addressDetails,
        EmployeeProfessionalDetailsResponse professionalDetails,
        List<EmployeeEducationResponse> educationDetails,
        List<EmployeeExperienceResponse> experienceDetails) {

    public static EmployeeSelfProfileResponse from(EmployeeBasicInfoResponse profile) {
        return new EmployeeSelfProfileResponse(
                profile.getEmployeeId(), profile.getEmployeeName(), profile.getFirstName(), profile.getLastName(),
                profile.getEmail(), profile.getPhoneNumber(), profile.getGender(), profile.getDateOfBirth(),
                profile.getRitId(), profile.getCsxRacfId(), profile.getEmploymentType(), profile.getJoiningDate(),
                profile.getWorkMode(), profile.getWorkLocation(), profile.getStatus(),
                profile.getRoleId(), profile.getRoleCode(), profile.getRoleName(),
                profile.getDesignationId(), profile.getDesignationName(), profile.getCurrentProjects(),
                profile.getAddressDetails(), profile.getProfessionalDetails(), profile.getEducationDetails(),
                profile.getExperienceDetails());
    }
}
