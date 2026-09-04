package com.rit.performance.repository;
import com.rit.performance.entity.SowInvoiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SowInvoiceHistoryRepository extends JpaRepository<SowInvoiceHistory, Long> {
    List<SowInvoiceHistory> findByInvoice_IdOrderByChangedOnDescIdDesc(Long invoiceId);
}
