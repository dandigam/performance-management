package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "employee_leave_policies")
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class EmployeeLeavePolicy extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_leave_policy_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_policy_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_leave_policy_policy"))
    private LeavePolicy leavePolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level1_approver_id", foreignKey = @ForeignKey(name = "fk_employee_leave_policy_l1"))
    private Employee level1Approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level2_approver_id", foreignKey = @ForeignKey(name = "fk_employee_leave_policy_l2"))
    private Employee level2Approver;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeavePolicyStatus status = LeavePolicyStatus.ACTIVE;
}
