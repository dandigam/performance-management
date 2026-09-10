package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(
                name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "timesheet_employee_projects",
        uniqueConstraints = @UniqueConstraint(name = "uq_employee_project_milestone",
                columnNames = {"employee_id", "sow_id", "milestone_id"}),
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tep_milestone"))
    private SowMilestone milestone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "default_hours_per_day", precision = 4, scale = 2)
    private BigDecimal defaultHoursPerDay;

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

    @OneToMany(mappedBy = "timesheetEmployeeProject", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("workDate ASC")
    @Builder.Default
    private List<TimesheetEmployeeProjectDay> dailySchedules = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void validateAssignment() {
        if (status == null) status = TimesheetEmployeeProjectStatus.ACTIVE;
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalStateException("endDate cannot be before startDate");
        }
        if (defaultHoursPerDay != null && (defaultHoursPerDay.signum() < 0
                || defaultHoursPerDay.compareTo(new BigDecimal("24.00")) > 0)) {
            throw new IllegalStateException("defaultHoursPerDay must be between 0 and 24");
        }
        if (employee != null && (sameEmployee(employee, level1Approver)
                || sameEmployee(employee, level2Approver))) {
            throw new IllegalStateException("An employee cannot approve their own timesheet");
        }
        if (sameEmployee(level1Approver, level2Approver)) {
            throw new IllegalStateException("Level 1 and Level 2 approvers must be different employees");
        }
    }

    /** Compatibility for the existing weekly timesheet response. */
    public Integer getMaxHoursPerDay() {
        return defaultHoursPerDay == null ? null : defaultHoursPerDay.intValue();
    }

    /** Compatibility for callers compiled against the former integer property. */
    public void setMaxHoursPerDay(Integer hours) {
        this.defaultHoursPerDay = hours == null ? null : BigDecimal.valueOf(hours);
    }

    private boolean sameEmployee(Employee first, Employee second) {
        return first != null && second != null && first.getId() != null
                && first.getId().equals(second.getId());
    }
}
