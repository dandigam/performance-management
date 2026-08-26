package com.rit.performance.repository;

import com.rit.performance.entity.SowMilestonePosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SowMilestonePositionRepository
        extends JpaRepository<SowMilestonePosition, Long> {
    @EntityGraph(attributePaths = {"sow", "milestone", "position", "rateCard"})
    Optional<SowMilestonePosition> findByIdAndMilestone_IdAndSow_Id(
            Long id, Long milestoneId, Long sowId);
}
