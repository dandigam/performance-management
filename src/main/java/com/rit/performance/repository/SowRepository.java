package com.rit.performance.repository;

import com.rit.performance.entity.Sow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SowRepository extends JpaRepository<Sow, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Sow s where upper(s.status.code) = 'DRAFT' and s.client.id = :clientId
            and exists (select m.id from SowMilestone m where m.sow = s
                and m.startDate <= :monthEnd and m.endDate >= :monthStart)
            order by s.id
            """)
    List<Sow> findDraftForHolidayMonths(
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("monthStart") java.time.LocalDate monthStart,
            @org.springframework.data.repository.query.Param("monthEnd") java.time.LocalDate monthEnd);

    @EntityGraph(attributePaths = {"status", "deliveryOwnerEmployee", "technicalLeadEmployee"})
    @Query("""
            select s from Sow s
            left join s.deliveryOwnerEmployee deliveryOwner
            left join s.technicalLeadEmployee technicalLead
            where deliveryOwner.id = :employeeId or technicalLead.id = :employeeId
            order by s.id
            """)
    List<Sow> findManagedByEmployeeId(
            @org.springframework.data.repository.query.Param("employeeId") Long employeeId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sow s where s.id = :id")
    Optional<Sow> findForStatusUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"businessUnit", "status", "deliveryOwnerEmployee", "technicalLeadEmployee"})
    @Query("select sow from Sow sow")
    Page<Sow> findSummaryPage(Pageable pageable);


    @EntityGraph(attributePaths = {
            "client", "businessUnit", "status", "ritContactEmployee", "ritEscalationEmployee",
            "deliveryOwnerEmployee", "technicalLeadEmployee",
            "milestones", "milestones.positions",
            "milestones.positions.position", "milestones.positions.skill",
            "milestones.positions.rateCard", "documents"
    })
    @Query("select distinct sow from Sow sow")
    List<Sow> findAllWithDetails();

    @EntityGraph(attributePaths = {
            "client", "businessUnit", "status", "ritContactEmployee", "ritEscalationEmployee",
            "deliveryOwnerEmployee", "technicalLeadEmployee",
            "milestones", "milestones.positions",
            "milestones.positions.position", "milestones.positions.skill", "documents"
    })
    @Query("select distinct sow from Sow sow where sow.id = :id")
    Optional<Sow> findByIdWithDetails(Long id);
}
