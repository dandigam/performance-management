package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

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

    @Column(length = 30)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "rit_id", unique = true, length = 50)
    private String ritId;

    @Column(name = "csx_racf_id", unique = true, length = 50)
    private String csxRacfId;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "work_mode", length = 50)
    private String workMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", foreignKey = @ForeignKey(name = "fk_employee_vendor"))
    private Vendor vendor;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "status", length = 20)
    private String status;
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmployeeDocument> employeeDocuments = new LinkedHashSet<>();
}
