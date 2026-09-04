package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_audit_history", indexes = @Index(
        name = "idx_employee_audit_employee_changed", columnList = "employee_id, changed_on"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAuditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    @Column(nullable = false, length = 30)
    private String action;
    @Column(name = "old_values", columnDefinition = "LONGTEXT")
    private String oldValues;
    @Column(name = "new_values", columnDefinition = "LONGTEXT")
    private String newValues;
    @Column(name = "changed_by")
    private Long changedBy;
    @Column(name = "changed_on", nullable = false)
    private LocalDateTime changedOn;
}
