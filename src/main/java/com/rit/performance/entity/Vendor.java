package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "vendor_location", length = 20)
    private String vendorLocation;

    @Column(name = "vendor_type", length = 50)
    private String vendorType;

    @Column(name = "tax_identifier", unique = true, length = 50)
    private String taxIdentifier;

    @Column(name = "primary_contact", length = 100)
    private String primaryContact;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String address;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vendor_documents",
            joinColumns = @JoinColumn(
                    name = "vendor_id",
                    foreignKey = @ForeignKey(name = "fk_vendor_documents_vendor")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "document_id",
                    foreignKey = @ForeignKey(name = "fk_vendor_documents_document")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_vendor_documents",
                    columnNames = {"vendor_id", "document_id"}
            )
    )
    @Builder.Default
    private Set<Document> documents = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
