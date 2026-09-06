package com.rit.performance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Table(name = "timesheet_approvals",
        uniqueConstraints = @UniqueConstraint(name = "uk_timesheet_approval_level",
                columnNames = {"timesheet_id", "approval_level"}),
        indexes = @Index(name = "idx_timesheet_approval_approver",
                columnList = "approver_employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TimesheetApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timesheet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_timesheet_approval_timesheet"))
    private Timesheet timesheet;

    @Min(1)
    @Max(2)
    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_timesheet_approval_approver"))
    private Employee approverEmployee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TimesheetApprovalStatus status = TimesheetApprovalStatus.PENDING;

    @Column(length = 2000)
    private String comments;

    @Column(name = "action_at")
    private LocalDateTime actionAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = TimesheetApprovalStatus.PENDING;
    }
}
