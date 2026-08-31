package com.rit.performance.mapper;

import com.rit.performance.dto.AuditResponse;
import com.rit.performance.entity.BaseEntity;

import java.util.Map;

public final class AuditMapper {
    private AuditMapper() {
    }

    public static AuditResponse toResponse(BaseEntity entity, Map<Long, String> auditorNames) {
        if (entity == null) {
            return null;
        }
        Map<Long, String> names = auditorNames == null ? Map.of() : auditorNames;
        return new AuditResponse(
                entity.getCreatedBy(),
                nameFor(names, entity.getCreatedBy()),
                entity.getCreatedOn(),
                entity.getUpdatedBy(),
                nameFor(names, entity.getUpdatedBy()),
                entity.getUpdatedOn());
    }

    private static String nameFor(Map<Long, String> names, Long userId) {
        return userId == null ? null : names.get(userId);
    }
}
