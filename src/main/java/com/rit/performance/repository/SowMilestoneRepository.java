package com.rit.performance.repository;

import com.rit.performance.entity.SowMilestone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SowMilestoneRepository extends JpaRepository<SowMilestone, Long> {
    Page<SowMilestone> findBySow_Id(Long sowId, Pageable pageable);

    Optional<SowMilestone> findByIdAndSow_Id(Long id, Long sowId);
}
