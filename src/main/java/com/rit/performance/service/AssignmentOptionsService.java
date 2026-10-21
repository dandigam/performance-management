package com.rit.performance.service;

import com.rit.performance.dto.AssignmentSowOptionResponse;
import com.rit.performance.dto.AssignmentSowOptionResponse.*;
import com.rit.performance.entity.SowMilestonePosition;
import com.rit.performance.repository.SowMilestonePositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentOptionsService {
    private final SowMilestonePositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<AssignmentSowOptionResponse> getSows() {
        var bySow = positionRepository.findAssignmentOptions().stream()
                .collect(Collectors.groupingBy(p -> p.getSow().getId(), LinkedHashMap::new, Collectors.toList()));
        return bySow.values().stream().map(positions -> {
            var sow = positions.get(0).getSow();
            var byMilestone = positions.stream().collect(Collectors.groupingBy(
                    p -> p.getMilestone().getId(), LinkedHashMap::new, Collectors.toList()));
            var milestones = byMilestone.values().stream().map(items -> {
                var milestone = items.get(0).getMilestone();
                return new MilestoneOption(milestone.getId(), milestone.getMilestoneName(),
                        milestone.getStartDate(), milestone.getEndDate(), items.stream().map(this::position).toList());
            }).toList();
            return new AssignmentSowOptionResponse(sow.getId(), sow.getSowName(), sow.getStatus().getCode(), milestones);
        }).toList();
    }

    private PositionOption position(SowMilestonePosition p) {
        return new PositionOption(p.getId(), p.getPositionName(), p.getPosition().getId(),
                p.getSkill() == null ? null : p.getSkill().getName(),
                p.getSeniority() == null ? null : p.getSeniority().getName(),
                p.getLocationType(), p.getPositionType(), p.getStatus(), p.getStartDate(), p.getEndDate(), numericHours(p.getHours()));
    }

    private BigDecimal numericHours(String hours) {
        if (hours == null || hours.isBlank()) return null;
        try { return new BigDecimal(hours.trim()); }
        catch (NumberFormatException ignored) { return null; } // Legacy free-text estimates have no numeric value.
    }
}
