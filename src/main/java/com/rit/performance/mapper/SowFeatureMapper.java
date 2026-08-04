package com.rit.performance.mapper;

import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.entity.SowFeature;

public final class SowFeatureMapper {
    private SowFeatureMapper() {
    }

    public static SowFeatureResponse toResponse(SowFeature feature) {
        return SowFeatureResponse.builder()
                .id(feature.getId())
                .sowId(feature.getSow().getId())
                .featureCode(feature.getFeatureCode())
                .featureName(feature.getFeatureName())
                .startDate(feature.getStartDate())
                .endDate(feature.getEndDate())
                .status(feature.getStatus())
                .completionPercentage(feature.getCompletionPercentage())
                .riskPercentage(feature.getRiskPercentage())
                .productivityPercentage(feature.getProductivityPercentage())
                .remarks(feature.getRemarks())
                .displayOrder(feature.getDisplayOrder())
                .active(feature.isActive())
                .createdBy(feature.getCreatedBy())
                .createdDate(feature.getCreatedDate())
                .updatedBy(feature.getUpdatedBy())
                .updatedDate(feature.getUpdatedDate())
                .build();
    }
}
