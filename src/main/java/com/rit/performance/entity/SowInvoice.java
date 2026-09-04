package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sow_invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sow_invoice_milestone", columnNames = "milestone_id"),
                @UniqueConstraint(name = "uk_sow_invoice_number", columnNames = "invoice_number")
        },
        indexes = {
                @Index(name = "idx_sow_invoices_sow_id", columnList = "sow_id"),
                @Index(name = "idx_sow_invoices_invoice_status", columnList = "invoice_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SowInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_invoice_sow"))
    private Sow sow;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sow_invoice_milestone"))
    private SowMilestone milestone;

    @Column(name = "milestone_invoice_date")
    private LocalDate milestoneInvoiceDate;

    @Column(name = "milestone_invoice_amount", precision = 15, scale = 2)
    private BigDecimal milestoneInvoiceAmount;

    @Column(name = "invoice_raised_date")
    private LocalDate invoiceRaisedDate;

    @Column(name = "invoice_raised_amount", precision = 15, scale = 2)
    private BigDecimal invoiceRaisedAmount;

    @Column(name = "invoice_status", nullable = false, length = 30)
    private String invoiceStatus;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentDate ASC, id ASC")
    @Builder.Default
    private List<SowInvoicePayment> payments = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (invoiceStatus == null || invoiceStatus.isBlank()) invoiceStatus = "EXPECTED";
    }
}
