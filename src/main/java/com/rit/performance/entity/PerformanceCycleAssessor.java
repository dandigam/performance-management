package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_cycle_assessors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PerformanceCycleAssessor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performance_cycle_id", nullable = false)
    private Long performanceCycleId;

    @Column(name = "assessor_name", nullable = false, length = 100)
    private String assessorName;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "action_type_id", nullable = false)
    private Long actionTypeId;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal weightage = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;
    @PrePersist
    public void prePersist() {
        if (weightage == null) {
            weightage = BigDecimal.ZERO;
        }
        if (active == null) {
            active = true;
        }
    }
}
