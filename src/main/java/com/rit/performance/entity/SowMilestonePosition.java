package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_milestone_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SowMilestonePosition extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_sow"))
    private Sow sow;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_milestone"))
    private SowMilestone milestone;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_position"))
    private LookupValue position;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id",
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_skill"))
    private LookupValue skill;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_card_id",
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_rate_card"))
    private RateCard rateCard;
    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate;
    @Column(name = "rate_override_reason", length = 1000)
    private String rateOverrideReason;
    @Column(name = "rate_updated_by")
    private Long rateUpdatedBy;
    @Column(name = "rate_updated_date")
    private LocalDateTime rateUpdatedDate;
    @Column(name = "position_name", nullable = false, length = 200)
    private String positionName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seniority_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_smp_seniority"))
    private LookupValue seniority;
    @Column(name = "position_type", nullable = false, length = 20)
    private String positionType;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "location_type", length = 20)
    private String locationType;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(length = 50)
    private String hours;
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @PrePersist
    void applyDefaults() {
        if (positionType == null || positionType.isBlank()) positionType = "BILLABLE";
        if (status == null || status.isBlank()) status = "OPEN";
    }
}
