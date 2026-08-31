package com.rit.performance.mapper;

import com.rit.performance.dto.CycleDetailsRequest;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.entity.PerformanceCycles;

public final class CycleDetailsMapper {

    private CycleDetailsMapper() {
    }

    public static PerformanceCycles toEntity(CycleDetailsRequest request) {
        if (request == null) {
            return null;
        }
        return PerformanceCycles.builder()
                .cycleName(request.getCycleName())
                .evaluationStartDate(request.getEvaluationStartDate())
                .evaluationEndDate(request.getEvaluationEndDate())
                .description(request.getDescription())
                .reviewTypeId(request.getReviewTypeId())
                .applicableTypeId(request.getApplicableTypeId())
                .scopeValueIds(request.getScopeValueIds())
                .status(request.getStatus() == null || request.getStatus().isBlank()
                        ? "DRAFT"
                        : request.getStatus())
                .build();
    }

    public static CycleDetailsResponse toResponse(PerformanceCycles entity) {
        if (entity == null) {
            return null;
        }
        return CycleDetailsResponse.builder()
                .id(entity.getId())
                .cycleName(entity.getCycleName())
                .evaluationStartDate(entity.getEvaluationStartDate())
                .evaluationEndDate(entity.getEvaluationEndDate())
                .description(entity.getDescription())
                .reviewTypeId(entity.getReviewTypeId())
                .applicableTypeId(entity.getApplicableTypeId())
                .scopeValueIds(entity.getScopeValueIds())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedDate(entity.getUpdatedOn())
                .build();
    }
}
