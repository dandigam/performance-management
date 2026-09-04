package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "employee_documents", uniqueConstraints = @UniqueConstraint(
        name = "uk_employee_documents", columnNames = {"employee_id", "document_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeDocument extends BaseEntity {

    @EmbeddedId
    private EmployeeDocumentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("employeeId")
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_documents_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("documentId")
    @JoinColumn(name = "document_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_documents_document"))
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id",
            foreignKey = @ForeignKey(name = "fk_employee_documents_document_type"))
    private LookupValue documentType;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
