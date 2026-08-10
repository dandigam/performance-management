package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
    @JsonAlias("ritEmployeeId")
    @Size(max = 50)
    private String ritId;
    @Size(max = 50)
    private String csxRacfId;
    @Size(max = 50)
    private String employmentType;
    @Size(max = 50)
    private String workMode;
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
    private Long departmentId;
    @JsonIgnore
    private boolean departmentIdPresent;
    @Positive
    private Long designationId;
    @JsonIgnore
    private boolean designationIdPresent;
    @Positive
    private Long managerId;
    @JsonIgnore
    private boolean managerIdPresent;
    @Positive
    private Long leadId;
    @JsonIgnore
    private boolean leadIdPresent;
    @Positive
    private Long projectId;
    @JsonIgnore
    private boolean projectIdPresent;
    @PastOrPresent
    private LocalDate assignmentEffectiveFrom;
    @jakarta.validation.Valid
    private ProjectAssignmentRequest projectAssignment;
    private Long updatedBy;

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
        this.roleIdPresent = true;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
        this.departmentIdPresent = true;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
        this.designationIdPresent = true;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
        this.managerIdPresent = true;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
        this.leadIdPresent = true;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        this.projectIdPresent = true;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
        this.vendorIdPresent = true;
    }
}
