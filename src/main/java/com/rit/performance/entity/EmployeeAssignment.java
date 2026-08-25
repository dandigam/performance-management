package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "sow_id")
    private Long sowId;

    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "position_type", length = 20)
    private String positionType;

    @Column(name = "allocation_percentage")
    private Integer allocationPercentage;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_primary_assignment", nullable = false)
    private Boolean isPrimaryAssignment;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", insertable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date", insertable = false, updatable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    void applyDefaults() {
        if (isPrimaryAssignment == null) isPrimaryAssignment = false;
    }
}
