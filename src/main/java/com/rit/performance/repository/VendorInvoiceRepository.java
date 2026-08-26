package com.rit.performance.repository;

import com.rit.performance.entity.VendorInvoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, Long> {
    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);
    boolean existsByInvoiceNumberIgnoreCaseAndIdNot(String invoiceNumber, Long id);

    @EntityGraph(attributePaths = {"workOrder", "vendor", "items"})
    List<VendorInvoice> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"workOrder", "vendor", "items"})
    Optional<VendorInvoice> findOneById(Long id);
}
