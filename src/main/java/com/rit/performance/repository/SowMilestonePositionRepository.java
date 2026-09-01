package com.rit.performance.repository;

import com.rit.performance.entity.SowMilestonePosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SowMilestonePositionRepository
        extends JpaRepository<SowMilestonePosition, Long> {
    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill", "rateCard"})
    Optional<SowMilestonePosition> findByIdAndMilestone_IdAndSow_Id(
            Long id, Long milestoneId, Long sowId);

    @EntityGraph(attributePaths = {"sow", "milestone", "position", "skill"})
    List<SowMilestonePosition> findBySowId(Long sowId);
}
