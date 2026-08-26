package com.rit.performance.mapper;

import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.entity.SowFeature;
import com.rit.performance.entity.SowMilestone;

public final class SowFeatureMapper {
    private SowFeatureMapper() {
    }

    public static SowFeatureResponse toResponse(SowFeature feature) {
        SowMilestone milestone = feature.getMilestone();
        return SowFeatureResponse.builder()
                .id(feature.getId())
                .sowId(feature.getSow().getId())
                .milestoneId(milestone == null ? null : milestone.getId())
                .milestoneName(milestone == null ? null : milestone.getMilestoneName())
                .milestoneStartDate(milestone == null ? null : milestone.getStartDate())
                .milestoneEndDate(milestone == null ? null : milestone.getEndDate())
                .featureCode(feature.getFeatureCode())
                .featureName(feature.getFeatureName())
                .description(feature.getDescription())
                .startDate(feature.getStartDate())
                .endDate(feature.getEndDate())
                .status(feature.getStatus())
                .createdBy(feature.getCreatedBy())
                .createdDate(feature.getCreatedDate())
                .updatedBy(feature.getUpdatedBy())
                .updatedDate(feature.getUpdatedDate())
                .build();
    }
}
