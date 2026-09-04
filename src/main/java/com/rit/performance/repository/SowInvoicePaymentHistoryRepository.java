package com.rit.performance.repository;
import com.rit.performance.entity.SowInvoicePaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SowInvoicePaymentHistoryRepository extends JpaRepository<SowInvoicePaymentHistory, Long> {
    List<SowInvoicePaymentHistory> findByInvoice_IdOrderByChangedOnDescIdDesc(Long invoiceId);
}
