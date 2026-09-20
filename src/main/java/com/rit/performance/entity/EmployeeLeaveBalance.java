package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "employee_leave_balances", uniqueConstraints = @UniqueConstraint(
        name = "uk_employee_leave_balance_assignment_type_year",
        columnNames = {"employee_leave_policy_id", "leave_type_id", "balance_year"}))
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class EmployeeLeaveBalance extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_leave_balance_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_leave_policy_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_leave_balance_assignment"))
    private EmployeeLeavePolicy employeeLeavePolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_leave_balance_type"))
    private LeaveType leaveType;

    @Column(name = "balance_year", nullable = false)
    private int balanceYear;

    @Column(name = "opening_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal entitled;

    @Column(name = "used", nullable = false, precision = 12, scale = 6)
    private BigDecimal used = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeavePolicyStatus status = LeavePolicyStatus.ACTIVE;
}
