package com.rit.performance.service;

import com.rit.performance.dto.SowResourceRequirementResponse;
import com.rit.performance.dto.SowResourceRequirementItemResponse;
import com.rit.performance.dto.SowResourceRequirementSummaryResponse;
import com.rit.performance.dto.SowRequirementMilestonesResponse;
import com.rit.performance.dto.response.SowPositionMilestoneResponse;
import com.rit.performance.entity.*;
import com.rit.performance.repository.SowMilestonePositionRepository;
import com.rit.performance.repository.SowResourceRequirementRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.CsxEmployeeRepository;
import com.rit.performance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Maintains the derived HR headcount plan for each SOW.
 *
 * <p>The source of truth is {@code sow_milestone_positions}. This service does not
 * trusts the source rows rather than manually adjusting headcount. Whenever
 * milestone-position data changes, it recalculates all groups and reconciles them
 * with the existing summary: matching groups are updated, new groups are inserted,
 * and groups that disappeared are deleted. This prevents stale records while
 * preserving IDs for unchanged groups.</p>
 *
 * <p>A resource group is identified by:</p>
 * <ul>
 *   <li>position ID</li>
 *   <li>skill ID</li>
 *   <li>seniority</li>
 *   <li>location</li>
 * </ul>
 *
 * <p>For example, all rows matching {@code Java Developer + Java + Senior +
 * Offshore} belong to one resource group. A row with Spring instead of Java,
 * Junior instead of Senior, or Onsite instead of Offshore belongs to a different
 * resource group. If the Java/Senior/Offshore combination appears twice in one
 * milestone, that milestone's count for the resource group is 2.</p>
 *
 * <p>Milestones are treated as sequential rather than concurrent. Required
 * headcount is therefore the largest count of a resource group in any one
 * milestone, not the sum across all milestones. For example, counts of 2, 1,
 * and 3 in three milestones produce {@code required_hc = 3}.</p>
 *
 * <p>Position and skill names are copied into the summary as display snapshots.
 * Their IDs remain the grouping keys. Positions without a skill are omitted
 * because a complete HR resource group requires a skill.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SowResourceRequirementServiceImpl implements SowResourceRequirementService {
    private final SowResourceRequirementRepository requirementRepository;
    private final SowMilestonePositionRepository positionRepository;
    private final SowRepository sowRepository;
    private final CsxEmployeeRepository csxEmployeeRepository;

    @Override
    @Transactional
    public void rebuild(Long sowId) {
        // Read the current source rows. An empty result ultimately leaves this SOW
        // with no summary rows, which is the expected behavior for an empty SOW.
        List<SowMilestonePosition> positions = positionRepository.findBySowId(sowId).stream()
                .filter(position -> position.getSkill() != null)
                .toList();

        // Stage 1: count each resource group independently inside each milestone.
        // The milestone ID is included here so positions in different milestones
        // are not added together.
        Map<MilestoneGroup, Integer> milestoneCounts = new HashMap<>();
        for (SowMilestonePosition position : positions) {
            ResourceGroup resourceGroup = group(position);
            milestoneCounts.merge(new MilestoneGroup(
                    position.getMilestone().getId(), resourceGroup), 1, Integer::sum);
        }

        // Stage 2: collapse the per-milestone counts by taking their maximum.
        // This implements MAX(M1 count, M2 count, ...), not SUM(...).
        Map<ResourceGroup, Integer> requiredHeadcounts = new HashMap<>();
        milestoneCounts.forEach((group, count) -> requiredHeadcounts.merge(
                group.resourceGroup(), count, Math::max));

        // Keep one source row per group so its display names can be copied into
        // the denormalized summary alongside the grouping IDs.
        Map<ResourceGroup, SowMilestonePosition> sources = new HashMap<>();
        positions.forEach(position -> sources.putIfAbsent(group(position), position));

        // Reconcile the calculation with existing rows. Removing each matched key
        // leaves only obsolete rows in this map after the loop.
        Map<ResourceGroup, SowResourceRequirement> existing =
                requirementRepository.findBySowId(sowId).stream()
                        .collect(java.util.stream.Collectors.toMap(this::group, value -> value));
        List<SowResourceRequirement> changed = new ArrayList<>();
        Sow sow = positions.isEmpty() ? null : positions.get(0).getSow();
        requiredHeadcounts.forEach((resourceGroup, requiredHc) -> {
            SowMilestonePosition source = sources.get(resourceGroup);
            SowResourceRequirement requirement = existing.remove(resourceGroup);
            if (requirement == null) {
                requirement = SowResourceRequirement.builder()
                        .sow(sow)
                        .positionId(resourceGroup.positionId())
                        .skillId(resourceGroup.skillId())
                        .seniority(resourceGroup.seniority())
                        .location(resourceGroup.location())
                        .build();
            }
            requirement.setPositionName(source.getPositionName().trim());
            requirement.setSkillName(source.getSkill().getName());
            requirement.setRequiredHc(requiredHc);
            changed.add(requirement);
        });

        // Delete only groups no longer present. Flush before inserting new groups
        // so a changed grouping key cannot conflict with the unique constraint.
        if (!existing.isEmpty()) {
            requirementRepository.deleteAll(existing.values());
            requirementRepository.flush();
        }
        if (!changed.isEmpty()) requirementRepository.saveAll(changed);
    }

    @Override
    @Transactional
    public void clear(Long sowId) {
        requirementRepository.deleteBySowId(sowId);
        requirementRepository.flush();
    }

    @Override
    public List<SowResourceRequirementResponse> getAll() {
        return requirementRepository
                .findAllByOrderBySowIdAscPositionIdAscSkillIdAscSeniorityAscLocationAsc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<SowResourceRequirementSummaryResponse> getAllBySow() {
        Map<Long, SowResourceRequirementSummaryResponse> summaries = new LinkedHashMap<>();
        List<SowResourceRequirement> requirements = requirementRepository
                .findAllByOrderBySowIdAscPositionIdAscSkillIdAscSeniorityAscLocationAsc();
        Set<Long> ownerIds = requirements.stream()
                .map(requirement -> requirement.getSow().getProjectOwnerEmployeeId())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, CsxEmployee> owners = csxEmployeeRepository.findAllById(ownerIds).stream()
                .collect(java.util.stream.Collectors.toMap(CsxEmployee::getId, owner -> owner));
        requirements
                .forEach(requirement -> {
                    Sow sow = requirement.getSow();
                    SowResourceRequirementSummaryResponse summary = summaries.computeIfAbsent(
                            sow.getId(), ignored -> toSummaryResponse(
                                    sow, owners.get(sow.getProjectOwnerEmployeeId()), new ArrayList<>()));
                    summary.getPositionInfo().add(toItemResponse(requirement));
                    summary.setTotalRequiredHc(
                            summary.getTotalRequiredHc() + requirement.getRequiredHc());
                });
        return new ArrayList<>(summaries.values());
    }

    @Override
    public SowResourceRequirementSummaryResponse getBySowId(Long sowId) {
        Sow sow = sowRepository.findById(sowId)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + sowId));
        List<SowResourceRequirementItemResponse> positionInfo = requirementRepository
                .findBySowId(sowId).stream()
                .sorted(Comparator.comparing(SowResourceRequirement::getPositionId)
                        .thenComparing(SowResourceRequirement::getSkillId)
                        .thenComparing(SowResourceRequirement::getSeniority)
                        .thenComparing(SowResourceRequirement::getLocation))
                .map(this::toItemResponse)
                .toList();
        CsxEmployee projectOwner = sow.getProjectOwnerEmployeeId() == null ? null
                : csxEmployeeRepository.findById(sow.getProjectOwnerEmployeeId()).orElse(null);
        return toSummaryResponse(sow, projectOwner, positionInfo);
    }

    @Override
    public SowRequirementMilestonesResponse getMilestonesByPosition(Long sowId, Long positionId) {
        SowResourceRequirement requirement = requirementRepository.findBySowId(sowId).stream()
                .filter(candidate -> Objects.equals(candidate.getPositionId(), positionId))
                .min(Comparator.comparing(SowResourceRequirement::getId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource requirement not found for SOW " + sowId
                                + " and position " + positionId));

        List<SowPositionMilestoneResponse> milestones = positionRepository.findBySowId(sowId)
                .stream()
                .filter(position -> matchesRequirement(position, requirement))
                .map(SowMilestonePosition::getMilestone)
                .collect(java.util.stream.Collectors.toMap(
                        SowMilestone::getId,
                        milestone -> milestone,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(SowMilestone::getStartDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SowMilestone::getId))
                .map(milestone -> SowPositionMilestoneResponse.builder()
                        .milestoneId(milestone.getId())
                        .milestoneName(milestone.getMilestoneName())
                        .startDate(milestone.getStartDate())
                        .endDate(milestone.getEndDate())
                        .status(milestone.getStatus())
                        .build())
                .toList();

        return SowRequirementMilestonesResponse.builder()
                .sowId(sowId)
                .requirementId(requirement.getId())
                .positionId(requirement.getPositionId())
                .positionName(requirement.getPositionName())
                .skillId(requirement.getSkillId())
                .skillName(requirement.getSkillName())
                .seniority(requirement.getSeniority())
                .location(requirement.getLocation())
                .milestones(milestones)
                .build();
    }

    private boolean matchesRequirement(
            SowMilestonePosition position, SowResourceRequirement requirement) {
        return position.getPosition() != null
                && Objects.equals(position.getPosition().getId(), requirement.getPositionId())
                && position.getSkill() != null
                && Objects.equals(position.getSkill().getId(), requirement.getSkillId())
                && Objects.equals(normalize(position.getSeniority()),
                        normalize(requirement.getSeniority()))
                && Objects.equals(normalize(position.getLocationType()),
                        normalize(requirement.getLocation()));
    }

    private SowResourceRequirementSummaryResponse toSummaryResponse(
            Sow sow, CsxEmployee projectOwner,
            List<SowResourceRequirementItemResponse> positionInfo) {
        return SowResourceRequirementSummaryResponse.builder()
                .sowId(sow.getId())
                .sowCode(sow.getSowCode())
                .sowName(sow.getSowName())
                .businessUnitId(sow.getBusinessUnit() == null
                        ? null : sow.getBusinessUnit().getId())
                .businessUnitName(sow.getBusinessUnit() == null
                        ? null : sow.getBusinessUnit().getName())
                .projectOwnerEmployeeId(sow.getProjectOwnerEmployeeId())
                .projectOwnerEmployeeName(employeeName(projectOwner))
                .startDate(sow.getStartDate())
                .endDate(sow.getEndDate())
                .totalRequiredHc(positionInfo.stream()
                        .map(SowResourceRequirementItemResponse::getRequiredHc)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum())
                .positionInfo(positionInfo)
                .build();
    }

    private String employeeName(CsxEmployee employee) {
        if (employee == null) return null;
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private SowResourceRequirementItemResponse toItemResponse(
            SowResourceRequirement requirement) {
        return SowResourceRequirementItemResponse.builder()
                .id(requirement.getId())
                .positionId(requirement.getPositionId())
                .positionName(requirement.getPositionName())
                .skillId(requirement.getSkillId())
                .skillName(requirement.getSkillName())
                .seniority(requirement.getSeniority())
                .location(requirement.getLocation())
                .requiredHc(requirement.getRequiredHc())
                .build();
    }

    private ResourceGroup group(SowMilestonePosition position) {
        // Normalize text dimensions so values such as "Senior" and " senior "
        // are treated as the same resource group.
        return new ResourceGroup(
                position.getPosition().getId(),
                position.getSkill().getId(),
                normalize(position.getSeniority()),
                normalize(position.getLocationType()));
    }

    private ResourceGroup group(SowResourceRequirement requirement) {
        return new ResourceGroup(
                requirement.getPositionId(),
                requirement.getSkillId(),
                normalize(requirement.getSeniority()),
                normalize(requirement.getLocation()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private SowResourceRequirementResponse toResponse(SowResourceRequirement requirement) {
        return SowResourceRequirementResponse.builder()
                .id(requirement.getId())
                .sowId(requirement.getSow().getId())
                .sowCode(requirement.getSow().getSowCode())
                .sowName(requirement.getSow().getSowName())
                .positionId(requirement.getPositionId())
                .positionName(requirement.getPositionName())
                .skillId(requirement.getSkillId())
                .skillName(requirement.getSkillName())
                .seniority(requirement.getSeniority())
                .location(requirement.getLocation())
                .requiredHc(requirement.getRequiredHc())
                .build();
    }

    private record ResourceGroup(
            Long positionId, Long skillId, String seniority, String location) { }
    private record MilestoneGroup(Long milestoneId, ResourceGroup resourceGroup) { }
}
