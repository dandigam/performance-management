package com.rit.performance.service;

import com.rit.performance.dto.PerformanceCycleRatingScaleRequest;
import com.rit.performance.entity.PerformanceCycleRatingScale;

import java.util.Optional;

public interface PerformanceCycleRatingScaleService {

    PerformanceCycleRatingScale saveForCycle(Long cycleId, PerformanceCycleRatingScaleRequest request);

    PerformanceCycleRatingScale getByCycleId(Long cycleId);

    Optional<PerformanceCycleRatingScale> findByCycleId(Long cycleId);

    void deleteByCycleId(Long cycleId);
}
