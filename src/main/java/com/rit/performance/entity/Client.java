package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "clients", uniqueConstraints =
        @UniqueConstraint(name = "uk_client_name", columnNames = "client_name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "client_address", nullable = false, length = 1000)
    private String clientAddress;

    @Column(name = "procurement_person_name", nullable = false, length = 150)
    private String procurementPersonName;

    @Column(name = "procurement_contact_number", nullable = false, length = 30)
    private String procurementContactNumber;

    @Column(name = "procurement_email", nullable = false, length = 150)
    private String procurementEmail;

    @Column(name = "invoice_submission_type", nullable = false, length = 20)
    private String invoiceSubmissionType;

    @Column(name = "invoice_submission_email", length = 150)
    private String invoiceSubmissionEmail;

    @Column(name = "vmo_name", nullable = false, length = 150)
    private String vmoName;

    @Column(name = "vmo_contact_number", nullable = false, length = 30)
    private String vmoContactNumber;

    @Column(name = "vmo_email", nullable = false, length = 150)
    private String vmoEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "client_documents",
            joinColumns = @JoinColumn(name = "client_id",
                    foreignKey = @ForeignKey(name = "fk_client_documents_client")),
            inverseJoinColumns = @JoinColumn(name = "document_id",
                    foreignKey = @ForeignKey(name = "fk_client_documents_document")),
            uniqueConstraints = @UniqueConstraint(name = "uk_client_documents",
                    columnNames = {"client_id", "document_id"}))
    @Builder.Default
    private Set<Document> documents = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
        if (status == null || status.isBlank()) status = "ACTIVE";
    }

    @PreUpdate
    void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
