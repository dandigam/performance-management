package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_review_answers", uniqueConstraints = @UniqueConstraint(
        name = "uk_assessment_question", columnNames = {"employee_review_assessment_id", "performance_cycle_question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeReviewAnswer extends BaseEntity {

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

    @Column(name = "section_snapshot_name", length = 200)
    private String sectionSnapshotName;

    @Column(name = "question_snapshot_text", length = 1000)
    private String questionSnapshotText;

    @Column(name = "response_type_snapshot", length = 30)
    private String responseTypeSnapshot;

    @Column(name = "required_snapshot")
    private Boolean requiredSnapshot;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
