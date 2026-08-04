package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "rit_id", unique = true, length = 50)
    private String ritId;

    @Column(name = "csx_racf_id", unique = true, length = 50)
    private String csxRacfId;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", insertable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date", insertable = false, updatable = false)
    private LocalDateTime updatedDate;
}
