package com.rit.performance.service;

import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PerformanceCycleConfigServiceImpl implements PerformanceCycleConfigService {

    private final PerformanceCycleConfigRepository repository;

    public PerformanceCycleConfigServiceImpl(PerformanceCycleConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public PerformanceCycles create(PerformanceCycles performanceCycleConfig) {
        validateEvaluationDates(performanceCycleConfig);
        if (repository.existsByCycleName(performanceCycleConfig.getCycleName())) {
            throw new IllegalArgumentException("cycleName must be unique");
        }
        if (performanceCycleConfig.getStatus() == null || performanceCycleConfig.getStatus().isBlank()) {
            performanceCycleConfig.setStatus("DRAFT");
        }
        return repository.save(performanceCycleConfig);
    }

    @Override
    public PerformanceCycles update(Long id, PerformanceCycles performanceCycleConfig) {
        validateEvaluationDates(performanceCycleConfig);
        PerformanceCycles existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PerformanceCycleConfig not found"));

        if (!existing.getCycleName().equals(performanceCycleConfig.getCycleName())
                && repository.existsByCycleName(performanceCycleConfig.getCycleName())) {
            throw new IllegalArgumentException("cycleName must be unique");
        }

        existing.setCycleName(performanceCycleConfig.getCycleName());
        if (performanceCycleConfig.getEvaluationStartDate() != null) {
            existing.setEvaluationStartDate(performanceCycleConfig.getEvaluationStartDate());
            existing.setEvaluationEndDate(performanceCycleConfig.getEvaluationEndDate());
        }
        existing.setDescription(performanceCycleConfig.getDescription());
        existing.setReviewTypeId(performanceCycleConfig.getReviewTypeId());
        existing.setApplicableTypeId(performanceCycleConfig.getApplicableTypeId());
        existing.setStatus(performanceCycleConfig.getStatus() == null || performanceCycleConfig.getStatus().isBlank()
                ? existing.getStatus()
                : performanceCycleConfig.getStatus());
        existing.setUpdatedBy(performanceCycleConfig.getUpdatedBy());

        return repository.save(existing);
    }

    private void validateEvaluationDates(PerformanceCycles cycle) {
        if (cycle.getEvaluationStartDate() == null || cycle.getEvaluationEndDate() == null) {
            throw new IllegalArgumentException(
                    "evaluationStartDate and evaluationEndDate are required");
        }
        if (cycle.getEvaluationEndDate().isBefore(cycle.getEvaluationStartDate())) {
            throw new IllegalArgumentException("evaluationEndDate cannot be before evaluationStartDate");
        }
    }
}
