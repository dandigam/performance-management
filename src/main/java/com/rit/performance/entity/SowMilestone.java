package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "sow_milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_milestone_sow"))
    private Sow sow;

    @Column(name = "milestone_name", nullable = false, length = 200)
    private String milestoneName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String status;

    @OneToMany(mappedBy = "milestone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SowMilestonePosition> positions = new LinkedHashSet<>();

    public void addPosition(SowMilestonePosition position) {
        positions.add(position);
        position.setMilestone(this);
        position.setSow(sow);
    }

    public void clearPositions() {
        positions.forEach(position -> {
            position.setMilestone(null);
            position.setSow(null);
        });
        positions.clear();
    }

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
    }

    @PreUpdate
    void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
