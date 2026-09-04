package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sow_invoice_payment_history", indexes = {
        @Index(name = "idx_payment_history_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_payment_history_payment_id", columnList = "payment_id")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowInvoicePaymentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SowInvoice invoice;
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(name = "received_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;
    @Column(name = "payment_reference", length = 100) private String paymentReference;
    @Column(name = "payment_method", length = 50) private String paymentMethod;
    @Column(length = 500) private String notes;
    @Column(nullable = false, length = 30) private String action;
    @Column(name = "changed_by") private Long changedBy;
    @Column(name = "changed_on", nullable = false) private LocalDateTime changedOn;
}
