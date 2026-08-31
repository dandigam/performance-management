package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "performance_cycles", uniqueConstraints = @UniqueConstraint(columnNames = "cycle_name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PerformanceCycles extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_name", nullable = false, unique = true, length = 200)
    private String cycleName;

    @Column(name = "evaluation_start_date")
    private LocalDate evaluationStartDate;

    @Column(name = "evaluation_end_date")
    private LocalDate evaluationEndDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "review_type_id", nullable = false)
    private Long reviewTypeId;

    @Column(name = "applicable_to_id", nullable = false)
    private Long applicableTypeId;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "performance_cycle_scope_values",
            joinColumns = @JoinColumn(name = "performance_cycle_id"))
    @Column(name = "scope_value_id", nullable = false)
    @Builder.Default
    private List<Long> scopeValueIds = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
    }
}
