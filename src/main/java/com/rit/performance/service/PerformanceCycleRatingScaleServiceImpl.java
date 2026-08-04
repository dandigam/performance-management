package com.rit.performance.service;

import com.rit.performance.dto.PerformanceCycleRatingScaleRequest;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.PerformanceCycleRatingScale;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.PerformanceCycleRatingScaleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PerformanceCycleRatingScaleServiceImpl implements PerformanceCycleRatingScaleService {

    private final PerformanceCycleRatingScaleRepository repository;
    private final PerformanceCycleConfigRepository cycleRepository;
    private final LookupValueRepository lookupValueRepository;

    public PerformanceCycleRatingScaleServiceImpl(
            PerformanceCycleRatingScaleRepository repository,
            PerformanceCycleConfigRepository cycleRepository,
            LookupValueRepository lookupValueRepository) {
        this.repository = repository;
        this.cycleRepository = cycleRepository;
        this.lookupValueRepository = lookupValueRepository;
    }

    @Override
    public PerformanceCycleRatingScale saveForCycle(
            Long cycleId, PerformanceCycleRatingScaleRequest request) {
        if (request.getCycleId() != null && !request.getCycleId().equals(cycleId)) {
            throw new IllegalArgumentException("ratingScale.cycleId must match cycleDetails.id");
        }
        if (!cycleRepository.existsById(cycleId)) {
            throw new IllegalArgumentException("Performance cycle not found: " + cycleId);
        }

        LookupValue ratingScale = resolveRatingScale(request.getRatingScaleId());

        PerformanceCycleRatingScale entity = repository.findByPerformanceCycleId(cycleId)
                .orElseGet(PerformanceCycleRatingScale::new);
        entity.setPerformanceCycleId(cycleId);
        entity.setScaleName(request.getScaleName());
        entity.setRatingScaleId(ratingScale.getId());
        entity.setActive(true);
        return repository.save(entity);
    }

    private LookupValue resolveRatingScale(Long requestedId) {
        LookupValue directMatch = lookupValueRepository.findById(requestedId).orElse(null);
        if (directMatch != null
                && directMatch.isActive()
                && directMatch.getLookupType().isActive()
                && "RATING_SCALE".equalsIgnoreCase(directMatch.getLookupType().getCode())) {
            return directMatch;
        }

        if (requestedId >= 1 && requestedId <= 3) {
            return lookupValueRepository
                    .findByLookupTypeCodeIgnoreCaseAndDisplayOrderAndLookupTypeActiveTrueAndActiveTrue(
                            "RATING_SCALE", requestedId.intValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No active rating scale configured for selection: " + requestedId));
        }

        throw new IllegalArgumentException(
                "ratingScaleId must be 1, 2, 3, or an active RATING_SCALE lookup ID");
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceCycleRatingScale getByCycleId(Long cycleId) {
        return findByCycleId(cycleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rating scale not found for performance cycle: " + cycleId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PerformanceCycleRatingScale> findByCycleId(Long cycleId) {
        return repository.findByPerformanceCycleId(cycleId);
    }

    @Override
    public void deleteByCycleId(Long cycleId) {
        repository.deleteByPerformanceCycleId(cycleId);
    }
}
