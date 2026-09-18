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
    @EntityGraph(attributePaths = {"sow", "sow.status", "milestone", "position", "skill", "seniority"})
    @org.springframework.data.jpa.repository.Query("""
            select p from SowMilestonePosition p
            where upper(p.sow.status.code) in ('DRAFT', 'ACTIVE')
              and upper(p.status) = 'OPEN'
              and p.milestone.sow.id = p.sow.id
              and (p.sow.endDate is null or p.sow.endDate >= :today)
              and (p.milestone.endDate is null or p.milestone.endDate >= :today)
              and (p.endDate is null or p.endDate >= :today)
              and not exists (select a.id from SowMilestonePositionAssignment a
                  where a.milestonePosition.id = p.id and upper(a.status) = 'ASSIGNED')
            order by p.sow.sowName, p.sow.id, p.milestone.startDate, p.milestone.id, p.positionName, p.id
            """)
    List<SowMilestonePosition> findAssignmentOptions(
            @org.springframework.data.repository.query.Param("today") java.time.LocalDate today);

    Page<SowMilestonePosition> findBySow_IdAndMilestone_Id(Long sowId, Long milestoneId, Pageable pageable);

    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill", "rateCard"})
    Optional<SowMilestonePosition> findByIdAndMilestone_IdAndSow_Id(
            Long id, Long milestoneId, Long sowId);

    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill"})
    List<SowMilestonePosition> findBySowId(Long sowId);
}
