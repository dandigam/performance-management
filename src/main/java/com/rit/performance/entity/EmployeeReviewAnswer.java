package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_review_answers", uniqueConstraints = @UniqueConstraint(
        name = "uk_assessment_question", columnNames = {"employee_review_assessment_id", "performance_cycle_question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_review_assessment_id", nullable = false)
    private EmployeeReviewAssessment employeeReviewAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_cycle_section_id", nullable = false)
    private PerformanceCycleSection performanceCycleSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_cycle_question_id", nullable = false)
    private PerformanceCycleQuestion performanceCycleQuestion;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDate == null) createdDate = now;
        updatedDate = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
