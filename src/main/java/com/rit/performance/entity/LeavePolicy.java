package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "leave_policies")
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class LeavePolicy extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private LeavePolicyCountry country;

    @Enumerated(EnumType.STRING) @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private LeavePolicyStatus status = LeavePolicyStatus.ACTIVE;
}
