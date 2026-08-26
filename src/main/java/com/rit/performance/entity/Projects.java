package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Projects {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true)
    private String projectCode;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String status;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}