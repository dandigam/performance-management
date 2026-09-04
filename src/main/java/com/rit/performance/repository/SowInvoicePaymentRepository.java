package com.rit.performance.repository;

import com.rit.performance.entity.SowInvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SowInvoicePaymentRepository extends JpaRepository<SowInvoicePayment, Long> {
    List<SowInvoicePayment> findByInvoice_IdOrderByPaymentDateAscIdAsc(Long invoiceId);
    Optional<SowInvoicePayment> findByIdAndInvoice_Id(Long id, Long invoiceId);
}
