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

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_cycle_rating_scales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PerformanceCycleRatingScale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performance_cycle_id", nullable = false)
    private Long performanceCycleId;

    @Column(name = "scale_name", nullable = false, length = 100)
    private String scaleName;

    @Column(name = "rating_scale_id", nullable = false)
    private Long ratingScaleId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;
    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
    }
}
