package com.rit.performance.entity;

import com.rit.performance.service.EmployeeReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_review_assessments", uniqueConstraints = @UniqueConstraint(
        name = "uk_review_assessment_level", columnNames = {"employee_review_id", "assessment_level"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeReviewAssessment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_review_id", nullable = false)
    private EmployeeReview employeeReview;

    @Column(name = "assessment_level", nullable = false)
    private Integer assessmentLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessor_role_id", nullable = false)
    private LookupValue assessorRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessor_employee_id", nullable = false)
    private Employee assessorEmployee;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @Builder.Default
    private EmployeeReviewStatus status = EmployeeReviewStatus.NOT_STARTED;

    @Column(name = "progress_percentage", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal progressPercentage = BigDecimal.ZERO;

    @Column(name = "overall_rating", precision = 5, scale = 2)
    private BigDecimal overallRating;
    @Column(name = "overall_comment", columnDefinition = "TEXT")
    private String overallComment;
    @Column(name = "started_date") private LocalDateTime startedDate;
    @Column(name = "submitted_date") private LocalDateTime submittedDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "reopen_reason", length = 2000) private String reopenReason;
    @Column(name = "reopened_date") private LocalDateTime reopenedDate;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_date", updatable = false) private LocalDateTime createdDate;
    @Column(name = "updated_by") private Long updatedBy;
    @Column(name = "updated_date") private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "employeeReviewAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<EmployeeReviewAnswer> answers = new ArrayList<>();

    @PrePersist void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDate == null) createdDate = now;
        if (progressPercentage == null) progressPercentage = BigDecimal.ZERO;
        updatedDate = now;
    }
    @PreUpdate void preUpdate() { updatedDate = LocalDateTime.now(); }
}
