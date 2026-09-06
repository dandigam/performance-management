package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(
                name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "timesheet_employee_projects",
        uniqueConstraints = @UniqueConstraint(name = "uk_timesheet_employee_project_start",
                columnNames = {"employee_id", "sow_id", "start_date"}),
        indexes = {
                @Index(name = "idx_tep_employee_status", columnList = "employee_id,status"),
                @Index(name = "idx_tep_sow_status", columnList = "sow_id,status"),
                @Index(name = "idx_tep_level1_approver", columnList = "level1_approver_id"),
                @Index(name = "idx_tep_level2_approver", columnList = "level2_approver_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TimesheetEmployeeProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tep_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tep_sow"))
    private Sow sow;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level1_approver_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tep_level1_approver"))
    private Employee level1Approver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level2_approver_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tep_level2_approver"))
    private Employee level2Approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TimesheetEmployeeProjectStatus status = TimesheetEmployeeProjectStatus.ACTIVE;

    @PrePersist
    @PreUpdate
    void validateAssignment() {
        if (status == null) status = TimesheetEmployeeProjectStatus.ACTIVE;
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalStateException("endDate cannot be before startDate");
        }
        if (employee != null && (sameEmployee(employee, level1Approver)
                || sameEmployee(employee, level2Approver))) {
            throw new IllegalStateException("An employee cannot approve their own timesheet");
        }
        if (sameEmployee(level1Approver, level2Approver)) {
            throw new IllegalStateException("Level 1 and Level 2 approvers must be different employees");
        }
    }

    private boolean sameEmployee(Employee first, Employee second) {
        return first != null && second != null && first.getId() != null
                && first.getId().equals(second.getId());
    }
}
