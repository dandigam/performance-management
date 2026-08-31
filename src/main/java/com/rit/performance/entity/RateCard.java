package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RateCard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "position_title_id", nullable = false)
    private Long positionTitleId;

    @Column(length = 100)
    private String skill;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "seniority_id")
    private Long seniorityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rate_card_client"))
    private Client client;

    @Column(name = "hourly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, length = 20)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 20)
    private String status;
    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) status = "ACTIVE";
    }
}
