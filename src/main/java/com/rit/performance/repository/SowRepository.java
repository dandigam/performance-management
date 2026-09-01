package com.rit.performance.repository;

import com.rit.performance.entity.Sow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SowRepository extends JpaRepository<Sow, Long> {
    boolean existsBySowCodeIgnoreCase(String sowCode);
    boolean existsBySowCodeIgnoreCaseAndIdNot(String sowCode, Long id);

    @EntityGraph(attributePaths = {
            "client", "businessUnit", "ritContactEmployee", "ritEscalationEmployee",
            "milestones", "milestones.positions",
            "milestones.positions.position", "milestones.positions.skill", "documents"
    })
    @Query("select distinct sow from Sow sow")
    List<Sow> findAllWithDetails();

    @EntityGraph(attributePaths = {
            "client", "businessUnit", "ritContactEmployee", "ritEscalationEmployee",
            "milestones", "milestones.positions",
            "milestones.positions.position", "milestones.positions.skill", "documents"
    })
    @Query("select distinct sow from Sow sow where sow.id = :id")
    Optional<Sow> findByIdWithDetails(Long id);
}
