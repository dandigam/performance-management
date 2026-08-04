package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@Builder
public class SowFeature {
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

    @Column(name = "feature_code", nullable = false, length = 50)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "risk_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskPercentage;

    @Column(name = "productivity_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal productivityPercentage;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
        if (status == null || status.isBlank()) status = "NOT_STARTED";
        if (completionPercentage == null) completionPercentage = BigDecimal.ZERO;
        if (riskPercentage == null) riskPercentage = BigDecimal.ZERO;
        if (productivityPercentage == null) productivityPercentage = BigDecimal.ZERO;
        if (displayOrder == null) displayOrder = 1;
        active = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
