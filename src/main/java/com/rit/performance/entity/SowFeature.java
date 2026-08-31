package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sow_features",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sow_feature_code",
                columnNames = {"sow_id", "feature_code"}
        ),
        indexes = {
                @Index(name = "idx_sow_features_sow_id", columnList = "sow_id"),
                @Index(name = "idx_sow_features_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SowFeature extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sow_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_features_sow")
    )
    private Sow sow;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(
            name = "milestone_id",
            foreignKey = @ForeignKey(name = "fk_sow_features_milestone")
    )
    private SowMilestone milestone;

    @Column(name = "feature_code", nullable = false, length = 50)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status;
    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) status = "TODO";
    }
}
