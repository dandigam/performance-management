package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_balance_adjustments")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class EmployeeLeaveBalanceAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_leave_balance_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_leave_balance_adjustment_balance"))
    private EmployeeLeaveBalance employeeLeaveBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 10)
    private LeaveBalanceAdjustmentType adjustmentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 1000)
    private String notes;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
}
