package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sow_resource_requirement", uniqueConstraints = @UniqueConstraint(
        name = "uk_resource_requirement",
        columnNames = {"sow_id", "position_id", "skill_id", "seniority", "location"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowResourceRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_resource_requirement_sow"))
    private Sow sow;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "position_name", nullable = false, length = 200)
    private String positionName;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "skill_name", nullable = false, length = 200)
    private String skillName;

    @Column(nullable = false, length = 50)
    private String seniority;

    @Column(nullable = false, length = 30)
    private String location;

    @Column(name = "required_hc", nullable = false)
    private Integer requiredHc;
}
