package com.rit.performance.repository;

import com.rit.performance.entity.SowMilestonePosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SowMilestonePositionRepository
        extends JpaRepository<SowMilestonePosition, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select p.hours from SowMilestonePosition p where p.milestone.id = :milestoneId
            """)
    List<String> findHoursByMilestoneId(
            @org.springframework.data.repository.query.Param("milestoneId") Long milestoneId);

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(p.amount), 0) from SowMilestonePosition p
            where p.milestone.id = :milestoneId
            """)
    java.math.BigDecimal sumAmountByMilestoneId(
            @org.springframework.data.repository.query.Param("milestoneId") Long milestoneId);

    @org.springframework.data.jpa.repository.Query("""
            select p from SowMilestonePosition p
            where p.sow.id = :sowId and upper(p.sow.status.code) = 'DRAFT'
              and p.milestone.startDate <= :holidayDate
              and p.milestone.endDate >= :holidayDate
              and (p.startDate is null or p.startDate <= :holidayDate)
              and (p.endDate is null or p.endDate >= :holidayDate)
            order by p.milestone.id, p.id
            """)
    List<SowMilestonePosition> findForHoliday(
            @org.springframework.data.repository.query.Param("sowId") Long sowId,
            @org.springframework.data.repository.query.Param("holidayDate") java.time.LocalDate holidayDate);

    @EntityGraph(attributePaths = {"sow", "sow.status", "milestone", "position", "skill", "seniority"})
    @org.springframework.data.jpa.repository.Query("""
            select p from SowMilestonePosition p
            where upper(p.status) = 'OPEN'
            order by p.sow.sowName, p.sow.id, p.milestone.startDate, p.milestone.id, p.positionName, p.id
            """)
    List<SowMilestonePosition> findAssignmentOptions();

    Page<SowMilestonePosition> findBySow_IdAndMilestone_Id(Long sowId, Long milestoneId, Pageable pageable);

    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill", "rateCard"})
    Optional<SowMilestonePosition> findByIdAndMilestone_IdAndSow_Id(
            Long id, Long milestoneId, Long sowId);

    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill"})
    List<SowMilestonePosition> findBySowId(Long sowId);
}
