package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_status_history", indexes = @Index(name = "idx_sow_status_history_sow", columnList = "sow_id,id"))
@Getter @Setter
public class SowStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "is_baseline", nullable = false, updatable = false)
    private boolean baseline = false;
    // Snapshot identifier: deliberately no FK so deleting a SOW preserves its audit trail.
    @Column(name = "sow_id", nullable = false, updatable = false)
    private Long sowId;
    @Column(name = "previous_status", length = 30, updatable = false)
    private String previousStatus;
    @Column(nullable = false, length = 30, updatable = false)
    private String status;
    @Column(name = "status_effective_date", updatable = false)
    private LocalDate statusEffectiveDate;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
    @Column(name = "changed_by", updatable = false)
    private Long changedBy;
    @Column(name = "approved_at", updatable = false)
    private LocalDateTime approvedAt;
}
