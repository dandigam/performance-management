package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_owner_history", indexes = @Index(name = "idx_sow_owner_history_sow", columnList = "sow_id,id"))
@Getter @Setter
public class SowOwnerHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Snapshot identifiers and names preserve the audit trail after source records change.
    @Column(name = "sow_id", nullable = false, updatable = false)
    private Long sowId;
    @Column(nullable = false, length = 30, updatable = false)
    private String role;
    @Column(name = "previous_employee_id", updatable = false)
    private Long previousEmployeeId;
    @Column(name = "previous_employee_name", length = 511, updatable = false)
    private String previousEmployeeName;
    @Column(name = "employee_id", updatable = false)
    private Long employeeId;
    @Column(name = "employee_name", length = 511, updatable = false)
    private String employeeName;
    @Column(name = "effective_date", updatable = false)
    private LocalDate effectiveDate;
    @Column(length = 2000, updatable = false)
    private String reason;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
    @Column(name = "changed_by", updatable = false)
    private Long changedBy;
    @Column(name = "is_baseline", nullable = false, updatable = false)
    private boolean baseline;
}
