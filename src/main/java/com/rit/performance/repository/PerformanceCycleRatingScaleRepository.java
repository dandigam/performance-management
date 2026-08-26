package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycleRatingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceCycleRatingScaleRepository
        extends JpaRepository<PerformanceCycleRatingScale, Long> {

    Optional<PerformanceCycleRatingScale> findByPerformanceCycleId(Long performanceCycleId);

    void deleteByPerformanceCycleId(Long performanceCycleId);
}
