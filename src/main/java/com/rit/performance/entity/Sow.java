package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class Sow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sow_code", length = 50)
    private String sowCode;

    @Column(name = "sow_name", nullable = false, length = 200)
    private String sowName;

    @Column(name = "sow_type", nullable = false, length = 100)
    private String sowType;

    @Column(name = "engagement_type", nullable = false, length = 100)
    private String engagementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id", foreignKey = @ForeignKey(name = "fk_sow_business_unit"))
    private LookupValue businessUnit;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "csx_project_id", length = 100)
    private String csxProjectId;

    @Column(name = "project_owner_employee_id")
    private Long projectOwnerEmployeeId;

    @Column(name = "csx_contact_employee_id")
    private Long csxContactEmployeeId;

    @Column(name = "csx_escalation_employee_id")
    private Long csxEscalationEmployeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rit_contact_employee_id", foreignKey = @ForeignKey(name = "fk_sow_rit_contact"))
    private Employee ritContactEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rit_escalation_employee_id",
            foreignKey = @ForeignKey(name = "fk_sow_rit_escalation"))
    private Employee ritEscalationEmployee;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 30)
    private String status;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "signed_status", length = 20)
    private String signedStatus;

    @Column(name = "signed_date")
    private LocalDate signedDate;
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
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (signedStatus == null || signedStatus.isBlank()) {
            signedStatus = "UNSIGNED";
        }
    }
}
