package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "work_orders", indexes = {
        @Index(name = "idx_work_order_sow_id", columnList = "sow_id"),
        @Index(name = "idx_work_order_employee_id", columnList = "employee_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_work_order_sow"))
    private Sow sow;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal salary;

    @Column(precision = 15, scale = 2)
    private BigDecimal commission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_work_order_employee"))
    private Employee employee;

    @Column(length = 2000)
    private String comments;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_order_documents",
            joinColumns = @JoinColumn(name = "work_order_id",
                    foreignKey = @ForeignKey(name = "fk_work_order_documents_work_order")),
            inverseJoinColumns = @JoinColumn(name = "document_id",
                    foreignKey = @ForeignKey(name = "fk_work_order_documents_document")),
            uniqueConstraints = @UniqueConstraint(name = "uk_work_order_documents",
                    columnNames = {"work_order_id", "document_id"})
    )
    @Builder.Default
    private Set<Document> documents = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
