package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sow_invoices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sow_invoice_milestone", columnNames = "milestone_id"),
        indexes = {
                @Index(name = "idx_sow_invoices_sow_id", columnList = "sow_id"),
                @Index(name = "idx_sow_invoices_invoice_status", columnList = "invoice_status"),
                @Index(name = "idx_sow_invoices_payment_status", columnList = "payment_status")
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

    @Column(name = "actual_invoice_date")
    private LocalDate actualInvoiceDate;

    @Column(name = "invoice_amount", precision = 15, scale = 2)
    private BigDecimal invoiceAmount;

    @Column(name = "invoice_status", nullable = false, length = 30)
    private String invoiceStatus;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "payment_received_date")
    private LocalDate paymentReceivedDate;

    @Column(name = "received_amount", precision = 15, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;
    @PrePersist
    void prePersist() {
        if (invoiceStatus == null || invoiceStatus.isBlank()) invoiceStatus = "DRAFT";
        if (paymentStatus == null || paymentStatus.isBlank()) paymentStatus = "UNPAID";
    }
}
