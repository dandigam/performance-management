package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_professional_profiles", uniqueConstraints =
        @UniqueConstraint(name = "uk_employee_professional_profile", columnNames = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfessionalProfile {
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
