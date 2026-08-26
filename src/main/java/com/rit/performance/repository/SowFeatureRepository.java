package com.rit.performance.repository;

import com.rit.performance.entity.SowFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SowFeatureRepository extends JpaRepository<SowFeature, Long> {
    List<SowFeature> findBySow_IdOrderByIdAsc(Long sowId);

    Optional<SowFeature> findByIdAndSow_Id(Long id, Long sowId);

    boolean existsBySow_IdAndFeatureCodeIgnoreCase(Long sowId, String featureCode);

    boolean existsBySow_IdAndFeatureCodeIgnoreCaseAndIdNot(
            Long sowId,
            String featureCode,
            Long id
    );

    boolean existsByMilestone_Id(Long milestoneId);
}
