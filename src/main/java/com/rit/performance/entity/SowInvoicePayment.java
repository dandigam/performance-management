package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sow_invoice_payments", indexes =
        @Index(name = "idx_invoice_payments_invoice_id", columnList = "invoice_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class SowInvoicePayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_invoice_payment_invoice"))
    private SowInvoice invoice;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "received_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(length = 500)
    private String notes;
}
