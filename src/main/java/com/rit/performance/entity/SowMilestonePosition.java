package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "sow_milestone_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestonePosition {
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
    @JoinColumn(name = "rate_card_id",
            foreignKey = @ForeignKey(name = "fk_sow_milestone_position_rate_card"))
    private RateCard rateCard;
    @Column(name = "position_name", nullable = false, length = 200)
    private String positionName;
    @Column(length = 100)
    private String seniority;
    @Column(name = "position_type", nullable = false, length = 20)
    private String positionType;
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
    }
}
