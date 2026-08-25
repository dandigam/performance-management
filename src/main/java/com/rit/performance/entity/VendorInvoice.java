package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendor_invoices", indexes = {
        @Index(name = "idx_vendor_invoice_work_order_id", columnList = "work_order_id"),
        @Index(name = "idx_vendor_invoice_vendor_id", columnList = "vendor_id")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_vendor_invoice_number", columnNames = "invoice_number"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "invoice_type", nullable = false, length = 50)
    private String invoiceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vendor_invoice_work_order"))
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vendor_invoice_vendor"))
    private Vendor vendor;

    @Column(nullable = false, length = 20)
    private String location;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "vendorInvoice", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<VendorInvoiceItem> items = new ArrayList<>();

    public void addItem(VendorInvoiceItem item) {
        items.add(item);
        item.setVendorInvoice(this);
    }

    public void clearItems() {
        items.forEach(item -> item.setVendorInvoice(null));
        items.clear();
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
