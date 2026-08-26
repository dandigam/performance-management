package com.rit.performance.service;

import com.rit.performance.entity.PerformanceCycles;

public interface PerformanceCycleConfigService {

    PerformanceCycles create(PerformanceCycles performanceCycleConfig);

    PerformanceCycles update(Long id, PerformanceCycles performanceCycleConfig);
}
