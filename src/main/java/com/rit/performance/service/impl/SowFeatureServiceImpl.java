package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowFeatureRequest;
import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.dto.response.SowProgressSummaryResponse;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowFeature;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.SowFeatureMapper;
import com.rit.performance.repository.SowFeatureRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.service.SowFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class SowFeatureServiceImpl implements SowFeatureService {
    private static final BigDecimal HIGH_RISK_THRESHOLD = BigDecimal.valueOf(70);
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "ON_HOLD", "BLOCKED", "CANCELLED"
    );

    private final SowRepository sowRepository;
    private final SowFeatureRepository featureRepository;

    @Override
    public SowFeatureResponse create(Long sowId, SowFeatureRequest request) {
        Sow sow = findSow(sowId);
        String featureCode = normalizeCode(request.getFeatureCode());
        validateUniqueCode(sowId, featureCode, null);
        validateDateRange(request);

        SowFeature feature = new SowFeature();
        feature.setSow(sow);
        feature.setFeatureCode(featureCode);
        apply(feature, request);
        return SowFeatureMapper.toResponse(featureRepository.save(feature));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SowFeatureResponse> getAll(Long sowId) {
        ensureSowExists(sowId);
        return findFeatures(sowId).stream()
                .map(SowFeatureMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SowFeatureResponse getById(Long sowId, Long featureId) {
        ensureSowExists(sowId);
        return SowFeatureMapper.toResponse(findFeature(sowId, featureId));
    }

    @Override
    public SowFeatureResponse update(Long sowId, Long featureId, SowFeatureRequest request) {
        ensureSowExists(sowId);
        SowFeature feature = findFeature(sowId, featureId);
        String featureCode = normalizeCode(request.getFeatureCode());
        validateUniqueCode(sowId, featureCode, featureId);
        validateDateRange(request);

        feature.setFeatureCode(featureCode);
        apply(feature, request);
        return SowFeatureMapper.toResponse(featureRepository.save(feature));
    }

    @Override
    public void delete(Long sowId, Long featureId) {
        ensureSowExists(sowId);
        featureRepository.delete(findFeature(sowId, featureId));
    }

    @Override
    @Transactional(readOnly = true)
    public SowProgressSummaryResponse getProgress(Long sowId) {
        ensureSowExists(sowId);
        List<SowFeature> features = findFeatures(sowId);
        int total = features.size();

        return SowProgressSummaryResponse.builder()
                .sowId(sowId)
                .overallCompletion(average(features, Metric.COMPLETION))
                .totalFeatures(total)
                .completedFeatures(countStatus(features, "COMPLETED"))
                .inProgressFeatures(countStatus(features, "IN_PROGRESS"))
                .notStartedFeatures(countStatus(features, "NOT_STARTED"))
                .highRiskFeatures((int) features.stream()
                        .filter(feature -> value(feature.getRiskPercentage())
                                .compareTo(HIGH_RISK_THRESHOLD) >= 0)
                        .count())
                .averageRisk(average(features, Metric.RISK))
                .averageProductivity(average(features, Metric.PRODUCTIVITY))
                .build();
    }

    private void apply(SowFeature feature, SowFeatureRequest request) {
        feature.setFeatureName(request.getFeatureName().trim());
        feature.setStartDate(request.getStartDate());
        feature.setEndDate(request.getEndDate());
        feature.setStatus(normalizeStatus(request.getStatus()));
        feature.setCompletionPercentage(value(request.getCompletionPercentage()));
        feature.setRiskPercentage(value(request.getRiskPercentage()));
        feature.setProductivityPercentage(value(request.getProductivityPercentage()));
        feature.setRemarks(trimToNull(request.getRemarks()));
        feature.setDisplayOrder(request.getDisplayOrder() == null ? 1 : request.getDisplayOrder());
        feature.setActive(true);
    }

    private Sow findSow(Long sowId) {
        return sowRepository.findById(sowId)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + sowId));
    }

    private void ensureSowExists(Long sowId) {
        if (!sowRepository.existsById(sowId)) {
            throw new ResourceNotFoundException("SOW not found: " + sowId);
        }
    }

    private SowFeature findFeature(Long sowId, Long featureId) {
        return featureRepository.findByIdAndSow_IdAndActiveTrue(featureId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Feature " + featureId + " not found for SOW " + sowId));
    }

    private List<SowFeature> findFeatures(Long sowId) {
        return featureRepository.findBySow_IdAndActiveTrueOrderByDisplayOrderAscIdAsc(sowId);
    }

    private void validateUniqueCode(Long sowId, String code, Long featureId) {
        boolean exists = featureId == null
                ? featureRepository.existsBySow_IdAndFeatureCodeIgnoreCase(sowId, code)
                : featureRepository.existsBySow_IdAndFeatureCodeIgnoreCaseAndIdNot(
                        sowId, code, featureId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Feature code already exists for SOW " + sowId + ": " + code);
        }
    }

    private void validateDateRange(SowFeatureRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException("Feature endDate cannot be before startDate");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "NOT_STARTED";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new InvalidOperationException(
                    "status must be one of NOT_STARTED, IN_PROGRESS, COMPLETED, ON_HOLD, BLOCKED, CANCELLED");
        }
        return normalized;
    }

    private int countStatus(List<SowFeature> features, String status) {
        return (int) features.stream()
                .filter(feature -> status.equalsIgnoreCase(feature.getStatus()))
                .count();
    }

    private BigDecimal average(List<SowFeature> features, Metric metric) {
        if (features.isEmpty()) return BigDecimal.ZERO.setScale(2);
        BigDecimal total = features.stream()
                .map(feature -> switch (metric) {
                    case COMPLETION -> value(feature.getCompletionPercentage());
                    case RISK -> value(feature.getRiskPercentage());
                    case PRODUCTIVITY -> value(feature.getProductivityPercentage());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(features.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private enum Metric {
        COMPLETION, RISK, PRODUCTIVITY
    }
}
