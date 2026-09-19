package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_request_days", uniqueConstraints = @UniqueConstraint(
        name = "uk_leave_request_day_date", columnNames = {"leave_request_id", "leave_date"}))
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class LeaveRequestDay extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_request_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_request_day_request"))
    private LeaveRequest leaveRequest;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Column(name = "scheduled_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal scheduledHours;

    @Column(name = "requested_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal requestedHours;
}
