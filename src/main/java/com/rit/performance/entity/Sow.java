package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "sows", uniqueConstraints = @UniqueConstraint(name = "uk_sow_code", columnNames = "sow_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sow_code", nullable = false, length = 50)
    private String sowCode;

    @Column(name = "sow_name", nullable = false, length = 200)
    private String sowName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id", foreignKey = @ForeignKey(name = "fk_sow_business_unit"))
    private LookupValue businessUnit;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "csx_project_id", length = 100)
    private String csxProjectId;

    @Column(name = "csx_contact_employee_id")
    private Long csxContactEmployeeId;

    @Column(name = "csx_escalation_employee_id")
    private Long csxEscalationEmployeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rit_contact_employee_id", foreignKey = @ForeignKey(name = "fk_sow_rit_contact"))
    private Employee ritContactEmployee;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 30)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "sow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SowMilestone> milestones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "sow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SowFeature> features = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sow_documents",
            joinColumns = @JoinColumn(
                    name = "sow_id",
                    foreignKey = @ForeignKey(name = "fk_sow_documents_sow")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "document_id",
                    foreignKey = @ForeignKey(name = "fk_sow_documents_document")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_sow_documents",
                    columnNames = {"sow_id", "document_id"}
            )
    )
    @Builder.Default
    private Set<Document> documents = new LinkedHashSet<>();

    public void addMilestone(SowMilestone milestone) {
        milestones.add(milestone);
        milestone.setSow(this);
    }

    public void removeMilestone(SowMilestone milestone) {
        milestones.remove(milestone);
        milestone.setSow(null);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
