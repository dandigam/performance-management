package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "final_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FinalRating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_review_id", nullable = false, unique = true)
    private EmployeeReview employeeReview;

    @Column(name = "final_rating", precision = 3, scale = 1, nullable = false)
    private BigDecimal finalRating;

    @Column(nullable = false)
    private Boolean published;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by", nullable = false)
    private User publishedBy;

    @PrePersist
    public void prePersist() {
        if (published == null) {
            published = false;
        }
    }
}
