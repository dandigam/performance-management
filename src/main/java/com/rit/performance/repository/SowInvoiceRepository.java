package com.rit.performance.repository;

import com.rit.performance.entity.SowInvoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SowInvoiceRepository extends JpaRepository<SowInvoice, Long> {

    boolean existsByMilestone_Id(Long milestoneId);

    Optional<SowInvoice> findByMilestone_Id(Long milestoneId);

    List<SowInvoice> findByMilestone_IdIn(Collection<Long> milestoneIds);

    @EntityGraph(attributePaths = {"sow", "sow.businessUnit", "milestone", "payments"})
    @Query("select distinct invoice from SowInvoice invoice")
    List<SowInvoice> findAllWithDetails();

    @EntityGraph(attributePaths = {"sow", "sow.businessUnit", "milestone", "payments"})
    @Query("select invoice from SowInvoice invoice where invoice.id = :id")
    Optional<SowInvoice> findByIdWithDetails(Long id);
}
