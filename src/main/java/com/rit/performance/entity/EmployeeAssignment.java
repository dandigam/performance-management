package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "sow_employee_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "sow_id")
    private Long sowId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

}
