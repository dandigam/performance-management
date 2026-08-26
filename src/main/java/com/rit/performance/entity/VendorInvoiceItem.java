package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vendor_invoices_items", indexes =
        @Index(name = "idx_vendor_invoice_item_invoice_id", columnList = "vendor_invoice_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_invoice_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vendor_invoice_item_invoice"))
    private VendorInvoice vendorInvoice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
