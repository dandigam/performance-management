package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_invoice_history", indexes =
        @Index(name = "idx_invoice_history_invoice_id", columnList = "invoice_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoiceHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SowInvoice invoice;
    @Column(name = "milestone_invoice_date") private LocalDate milestoneInvoiceDate;
    @Column(name = "milestone_invoice_amount", precision = 15, scale = 2)
    private BigDecimal milestoneInvoiceAmount;
    @Column(name = "invoice_raised_date") private LocalDate invoiceRaisedDate;
    @Column(name = "invoice_raised_amount", precision = 15, scale = 2)
    private BigDecimal invoiceRaisedAmount;
    @Column(name = "invoice_number", length = 100) private String invoiceNumber;
    @Column(name = "invoice_status", nullable = false, length = 30) private String invoiceStatus;
    @Column(name = "submitted_date") private LocalDate submittedDate;
    @Column(length = 500) private String notes;
    @Column(nullable = false, length = 30) private String action;
    @Column(name = "changed_by") private Long changedBy;
    @Column(name = "changed_on", nullable = false) private LocalDateTime changedOn;
}
