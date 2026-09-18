package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Table(name = "timesheets",
        uniqueConstraints = @UniqueConstraint(name = "uk_timesheet_employee_setup_week",
                columnNames = {"employee_id", "timesheet_employee_project_id", "week_start_date"}),
        indexes = @Index(name = "idx_timesheet_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Timesheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_timesheet_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_employee_project_id",
            foreignKey = @ForeignKey(name = "fk_timesheet_project_setup"))
    private TimesheetEmployeeProject timesheetEmployeeProject;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "regular_hours", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Column(name = "holiday_hours", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal holidayHours = BigDecimal.ZERO;

    @Column(name = "leave_hours", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal leaveHours = BigDecimal.ZERO;

    @Column(name = "total_hours", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal totalHours = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TimesheetStatus status = TimesheetStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("workDate ASC, id ASC")
    @Builder.Default
    private List<TimesheetEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("approvalLevel ASC")
    @Builder.Default
    private List<TimesheetApproval> approvals = new ArrayList<>();

    public void addEntry(TimesheetEntry entry) {
        entries.add(entry);
        entry.setTimesheet(this);
    }

    public void addApproval(TimesheetApproval approval) {
        approvals.add(approval);
        approval.setTimesheet(this);
    }

    public void recalculateHours() {
        regularHours = hoursFor(TimesheetEntryType.REGULAR);
        holidayHours = hoursFor(TimesheetEntryType.HOLIDAY);
        leaveHours = hoursFor(TimesheetEntryType.LEAVE);
        totalHours = regularHours.add(holidayHours).add(leaveHours);
    }

    private BigDecimal hoursFor(TimesheetEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(TimesheetEntry::getHours)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    @PreUpdate
    void validateWeek() {
        if (status == null) status = TimesheetStatus.DRAFT;
        if (weekStartDate != null && weekEndDate != null
                && !weekEndDate.equals(weekStartDate.plusDays(6))) {
            throw new IllegalStateException("weekEndDate must be six days after weekStartDate");
        }
    }
}
