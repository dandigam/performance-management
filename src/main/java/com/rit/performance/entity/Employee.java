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

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "rit_id")
    private String ritId;

    @Column(name = "csx_racf_id")
    private String csxRacfId;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "work_mode")
    private String workMode;

    @Column(name = "work_location",columnDefinition = "VARCHAR(20)")
    private String workLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", foreignKey = @ForeignKey(name = "fk_employee_vendor"))
    private Vendor vendor;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "status")
    private String status;
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmployeeDocument> employeeDocuments = new LinkedHashSet<>();
}
