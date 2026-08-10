package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ProjectAssignmentRequest {
    @Positive
    private Long departmentId;
    @Positive
    private Long projectId;
    @Positive
    private Long leadId;
    @Positive
    private Long managerId;
    @PastOrPresent
    private LocalDate effectiveFrom;
    private LocalDate assignmentEndDate;
    @Min(1) @Max(100)
    private Integer allocationPercentage;
    @Size(max = 20)
    private String status;

    @JsonIgnore private boolean departmentIdPresent;
    @JsonIgnore private boolean projectIdPresent;
    @JsonIgnore private boolean leadIdPresent;
    @JsonIgnore private boolean managerIdPresent;

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
        this.departmentIdPresent = true;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        this.projectIdPresent = true;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
        this.leadIdPresent = true;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
        this.managerIdPresent = true;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public void setAssignmentEndDate(LocalDate assignmentEndDate) {
        this.assignmentEndDate = assignmentEndDate;
    }

    public void setAllocationPercentage(Integer allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
