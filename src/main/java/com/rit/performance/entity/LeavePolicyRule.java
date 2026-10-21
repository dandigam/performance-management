package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_policy_rules", uniqueConstraints = @UniqueConstraint(
        name = "uk_leave_policy_rule_type", columnNames = {"leave_policy_id", "leave_type_id"}))
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class LeavePolicyRule extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_policy_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_policy_rule_policy"))
    private LeavePolicy leavePolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_policy_rule_type"))
    private LeaveType leaveType;

    @Column(precision = 10, scale = 2)
    private BigDecimal entitlement;

    @Column(name = "carry_forward", nullable = false)
    private boolean carryForward;

    @Column(name = "max_carry_forward", precision = 10, scale = 2)
    private BigDecimal maxCarryForward;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private LeavePolicyStatus status = LeavePolicyStatus.ACTIVE;
}
