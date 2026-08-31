package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_cycle_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PerformanceCycleQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performance_cycle_section_id", nullable = false)
    private Long performanceCycleSectionId;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "response_type", nullable = false, length = 30)
    private String responseType;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean required = true;

    @Column(name = "allow_comments")
    @Builder.Default
    private Boolean allowComments = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;
    @PrePersist
    public void prePersist() {
        if (required == null) {
            required = true;
        }
        if (allowComments == null) {
            allowComments = false;
        }
        if (active == null) {
            active = true;
        }
    }
}
