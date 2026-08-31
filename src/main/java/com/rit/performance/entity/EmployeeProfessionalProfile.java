package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_professional_profiles", uniqueConstraints =
        @UniqueConstraint(name = "uk_employee_professional_profile", columnNames = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeProfessionalProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_professional_profiles_employee"))
    private Employee employee;

    @Column(name = "it_skills", columnDefinition = "TEXT")
    private String itSkills;

    @Column(name = "latest_experience", columnDefinition = "TEXT")
    private String latestExperience;
}
