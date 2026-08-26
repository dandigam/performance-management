package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowFeatureRequest;
import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowFeature;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.SowFeatureMapper;
import com.rit.performance.repository.SowFeatureRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.service.SowFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class SowFeatureServiceImpl implements SowFeatureService {
    private static final Pattern GENERATED_CODE = Pattern.compile("F(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "TODO", "IN_PROGRESS", "PAUSE", "STOP", "COMPLETED"
    );

    private final SowRepository sowRepository;
    private final SowFeatureRepository featureRepository;
    private final SowMilestoneRepository milestoneRepository;

    @Override
    public SowFeatureResponse create(Long sowId, SowFeatureRequest request) {
        Sow sow = findSow(sowId);
        String featureCode = nextFeatureCode(sowId);
        SowMilestone milestone = findMilestone(sowId, request.getMilestoneId());
        validateDateRange(request, milestone);

        SowFeature feature = new SowFeature();
        feature.setSow(sow);
        feature.setMilestone(milestone);
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
        SowMilestone milestone = findMilestone(sowId, request.getMilestoneId());
        validateDateRange(request, milestone);

        feature.setMilestone(milestone);
        apply(feature, request);
        return SowFeatureMapper.toResponse(featureRepository.save(feature));
    }

    @Override
    public void delete(Long sowId, Long featureId) {
        ensureSowExists(sowId);
        featureRepository.delete(findFeature(sowId, featureId));
    }

    private void apply(SowFeature feature, SowFeatureRequest request) {
        feature.setFeatureName(request.getFeatureName().trim());
        feature.setDescription(trimToNull(request.getDescription()));
        feature.setStartDate(request.getStartDate());
        feature.setEndDate(request.getEndDate());
        feature.setStatus(normalizeStatus(request.getStatus()));
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
        return featureRepository.findByIdAndSow_Id(featureId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Feature " + featureId + " not found for SOW " + sowId));
    }

    private List<SowFeature> findFeatures(Long sowId) {
        return featureRepository.findBySow_IdOrderByIdAsc(sowId);
    }

    private SowMilestone findMilestone(Long sowId, Long milestoneId) {
        return milestoneRepository.findByIdAndSow_Id(milestoneId, sowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone " + milestoneId + " not found for SOW " + sowId));
    }

    private void validateDateRange(SowFeatureRequest request, SowMilestone milestone) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException("Feature endDate cannot be before startDate");
        }
        if (request.getStartDate() != null && milestone.getStartDate() != null
                && request.getStartDate().isBefore(milestone.getStartDate())) {
            throw new InvalidOperationException("Feature startDate cannot be before milestone startDate");
        }
        if (request.getEndDate() != null && milestone.getEndDate() != null
                && request.getEndDate().isAfter(milestone.getEndDate())) {
            throw new InvalidOperationException("Feature endDate cannot be after milestone endDate");
        }
    }

    private String nextFeatureCode(Long sowId) {
        int next = findFeatures(sowId).stream()
                .map(SowFeature::getFeatureCode)
                .filter(Objects::nonNull)
                .map(GENERATED_CODE::matcher)
                .filter(Matcher::matches)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max()
                .orElse(0) + 1;
        String code = "F%03d".formatted(next);
        while (featureRepository.existsBySow_IdAndFeatureCodeIgnoreCase(sowId, code)) {
            code = "F%03d".formatted(++next);
        }
        return code;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "TODO";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new InvalidOperationException(
                    "status must be one of TODO, IN_PROGRESS, PAUSE, STOP, COMPLETED");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
