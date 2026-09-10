package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Table(name = "timesheet_employee_project_day",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_work_date",
                columnNames = {"timesheet_employee_project_id", "work_date"}),
        indexes = {
                @Index(name = "idx_employee_work_date", columnList = "employee_id,work_date"),
                @Index(name = "idx_sow_work_date", columnList = "sow_id,work_date"),
                @Index(name = "idx_milestone_work_date", columnList = "milestone_id,work_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class TimesheetEmployeeProjectDay extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timesheet_employee_project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_day_configuration"))
    private TimesheetEmployeeProject timesheetEmployeeProject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false)
    private Sow sow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private SowMilestone milestone;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "scheduled_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal scheduledHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 30)
    private TimesheetDayType dayType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holiday_id")
    private Holiday holiday;

    @Column(name = "work_schedule_id")
    private Long workScheduleId;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist @PreUpdate
    void validate() {
        if (scheduledHours == null || scheduledHours.signum() < 0
                || scheduledHours.compareTo(new BigDecimal("24.00")) > 0) {
            throw new IllegalStateException("scheduledHours must be between 0 and 24");
        }
    }
}
