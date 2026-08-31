package com.rit.performance.entity;

import com.rit.performance.service.EmployeeReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uk_employee_review_employee_cycle", columnNames = {"employee_id", "cycle_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private PerformanceCycles performanceCycle;

    @Column(name = "sow_id")
    private Long sowId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EmployeeReviewStatus status = EmployeeReviewStatus.DRAFT;

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercentage = BigDecimal.ZERO;
    @Column(name = "extension_days_per_stage")
    private Integer extensionDaysPerStage;

    @Column(name = "extension_reason", length = 2000)
    private String extensionReason;

    @Column(name = "extension_granted_date")
    private LocalDateTime extensionGrantedDate;

    @OneToMany(mappedBy = "employeeReview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("assessmentLevel ASC")
    @Builder.Default
    private List<EmployeeReviewAssessment> assessments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (status == null) status = EmployeeReviewStatus.NOT_STARTED;
        if (progressPercentage == null) progressPercentage = BigDecimal.ZERO;
    }
}
