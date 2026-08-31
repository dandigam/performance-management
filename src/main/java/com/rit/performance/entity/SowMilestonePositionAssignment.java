package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_milestone_position_assignments", indexes = {
        @Index(name = "idx_smpa_employee_assignment_id", columnList = "employee_assignment_id"),
        @Index(name = "idx_smpa_milestone_position_id", columnList = "milestone_position_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class SowMilestonePositionAssignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_assignment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_smpa_employee_assignment"))
    private EmployeeAssignment employeeAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_position_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_smpa_milestone_position"))
    private SowMilestonePosition milestonePosition;

    @Column(name = "allocation_percentage", nullable = false)
    private Integer allocationPercentage;

    @Column(name = "position_type", nullable = false, length = 20)
    private String positionType;

    @Column(name = "assignment_start_date", nullable = false)
    private LocalDate assignmentStartDate;

    @Column(name = "assignment_end_date")
    private LocalDate assignmentEndDate;

    @Column(nullable = false, length = 20)
    private String status;
    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) status = "ACTIVE";
    }
}
