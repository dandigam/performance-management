package com.rit.performance.service;

import com.rit.performance.entity.PerformanceCycleAssessor;

import java.util.List;

public interface PerformanceCycleAssessorService {

    PerformanceCycleAssessor create(PerformanceCycleAssessor assessor);

    PerformanceCycleAssessor getById(Long id);

    List<PerformanceCycleAssessor> getByPerformanceCycleId(Long performanceCycleId);

    PerformanceCycleAssessor update(Long id, PerformanceCycleAssessor assessor);

    void delete(Long id);
}
