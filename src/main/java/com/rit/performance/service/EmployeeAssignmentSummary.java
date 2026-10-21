package com.rit.performance.service;

import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Response-only summary; the owning records remain the SOW and its positions. */
@Getter
@RequiredArgsConstructor
public class EmployeeAssignmentSummary {
    private final Long departmentId;
    private final Long designationId;
    private final Long milestoneId;
    private final String positionType;

    public static EmployeeAssignmentSummary from(Sow sow, List<SowMilestonePositionAssignment> assignments) {
        List<SowMilestonePositionAssignment> active = assignments.stream()
                .filter(item -> ("ASSIGNED".equalsIgnoreCase(item.getStatus())))
                .filter(item -> item.getMilestonePosition() != null).toList();
        return new EmployeeAssignmentSummary(
                sow == null || sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getId(),
                unique(active, item -> item.getMilestonePosition().getPosition() == null ? null
                        : item.getMilestonePosition().getPosition().getId()),
                unique(active, item -> item.getMilestonePosition().getMilestone() == null ? null
                        : item.getMilestonePosition().getMilestone().getId()),
                unique(active, SowMilestonePositionAssignment::getPositionType));
    }

    private static <T> T unique(List<SowMilestonePositionAssignment> assignments,
                                Function<SowMilestonePositionAssignment, T> value) {
        List<T> values = assignments.stream().map(value).filter(Objects::nonNull).distinct().toList();
        return values.size() == 1 ? values.get(0) : null;
    }
}
