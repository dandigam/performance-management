package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.SowMilestonePositionRepository;
import com.rit.performance.repository.SowResourceRequirementRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.CsxEmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SowResourceRequirementServiceImplTest {
    @Mock SowResourceRequirementRepository requirementRepository;
    @Mock SowMilestonePositionRepository positionRepository;
    @Mock SowRepository sowRepository;
    @Mock CsxEmployeeRepository csxEmployeeRepository;

    @Test
    void groupsBySkillAndUsesMaximumMilestoneCount() {
        Sow sow = Sow.builder().id(7L).build();
        LookupValue title = LookupValue.builder().id(21L).name("Developer").build();
        LookupValue javaSkill = LookupValue.builder().id(89L).name("Java").build();
        SowMilestone m1 = SowMilestone.builder().id(1L).build();
        SowMilestone m2 = SowMilestone.builder().id(2L).build();
        when(positionRepository.findBySowId(7L)).thenReturn(List.of(
                position(sow, m1, title, javaSkill), position(sow, m1, title, javaSkill),
                position(sow, m2, title, javaSkill)));
        SowResourceRequirementServiceImpl service =
                new SowResourceRequirementServiceImpl(
                        requirementRepository, positionRepository, sowRepository,
                        csxEmployeeRepository);

        service.rebuild(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SowResourceRequirement>> captor = ArgumentCaptor.forClass(List.class);
        verify(requirementRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(89L, captor.getValue().get(0).getSkillId());
        assertEquals("Java", captor.getValue().get(0).getSkillName());
        assertEquals(2, captor.getValue().get(0).getRequiredHc());
    }

    @Test
    void returnsRequirementsGroupedUnderTheirSowHeader() {
        LookupValue department = LookupValue.builder().id(5L).name("Engineering").build();
        Sow firstSow = Sow.builder().id(7L).sowCode("SOW-7").sowName("First")
                .businessUnit(department).projectOwnerEmployeeId(50L).build();
        Sow secondSow = Sow.builder().id(8L).sowCode("SOW-8").sowName("Second").build();
        when(csxEmployeeRepository.findAllById(
                org.mockito.ArgumentMatchers.<Long>anySet()))
                .thenReturn(List.of(CsxEmployee.builder()
                        .id(50L).firstName("Pat").lastName("Owner").build()));
        when(requirementRepository
                .findAllByOrderBySowIdAscPositionIdAscSkillIdAscSeniorityAscLocationAsc())
                .thenReturn(List.of(
                        requirement(1L, firstSow, 21L),
                        requirement(2L, firstSow, 82L),
                        requirement(3L, secondSow, 21L)));
        SowResourceRequirementServiceImpl service =
                new SowResourceRequirementServiceImpl(
                        requirementRepository, positionRepository, sowRepository,
                        csxEmployeeRepository);

        var result = service.getAllBySow();

        assertEquals(2, result.size());
        assertEquals(7L, result.get(0).getSowId());
        assertEquals("SOW-7", result.get(0).getSowCode());
        assertEquals("Engineering", result.get(0).getBusinessUnitName());
        assertEquals("Pat Owner", result.get(0).getProjectOwnerEmployeeName());
        assertEquals(2, result.get(0).getTotalRequiredHc());
        assertEquals(2, result.get(0).getPositionInfo().size());
        assertEquals(8L, result.get(1).getSowId());
        assertEquals(1, result.get(1).getPositionInfo().size());
    }

    @Test
    void returnsRequirementDetailsWithMatchingMilestones() {
        Sow sow = Sow.builder().id(20L).build();
        LookupValue title = LookupValue.builder().id(21L).name("Technical Lead").build();
        LookupValue javaSkill = LookupValue.builder().id(89L).name("Java").build();
        SowResourceRequirement requirement = SowResourceRequirement.builder()
                .id(1L).sow(sow).positionId(21L).positionName("Technical Lead")
                .skillId(89L).skillName("Java").seniority("SENIOR")
                .location("ONSITE").requiredHc(2).build();
        SowMilestone matchingMilestone = SowMilestone.builder().id(122L)
                .milestoneName("Milestone 1").status("PLANNING")
                .startDate(java.time.LocalDate.of(2026, 9, 1))
                .endDate(java.time.LocalDate.of(2026, 9, 15)).build();
        SowMilestone otherMilestone = SowMilestone.builder().id(123L)
                .milestoneName("Milestone 2").status("PLANNING").build();
        SowMilestonePosition matchingPosition = position(sow, matchingMilestone, title, javaSkill);
        matchingPosition.setLocationType("Onsite");
        SowMilestonePosition otherPosition = position(sow, otherMilestone, title, javaSkill);
        when(requirementRepository.findBySowId(20L)).thenReturn(List.of(requirement));
        when(positionRepository.findBySowId(20L))
                .thenReturn(List.of(matchingPosition, otherPosition));
        SowResourceRequirementServiceImpl service =
                new SowResourceRequirementServiceImpl(
                        requirementRepository, positionRepository, sowRepository,
                        csxEmployeeRepository);

        var result = service.getMilestonesByPosition(20L, 21L);

        assertEquals(20L, result.getSowId());
        assertEquals(1L, result.getRequirementId());
        assertEquals("Technical Lead", result.getPositionName());
        assertEquals(1, result.getMilestones().size());
        assertEquals(122L, result.getMilestones().get(0).getMilestoneId());
    }

    private SowResourceRequirement requirement(Long id, Sow sow, Long positionId) {
        return SowResourceRequirement.builder()
                .id(id).sow(sow).positionId(positionId).positionName("Developer")
                .skillId(89L).skillName("Java").seniority("SENIOR")
                .location("OFFSHORE").requiredHc(1).build();
    }

    private SowMilestonePosition position(Sow sow, SowMilestone milestone,
            LookupValue title, LookupValue skill) {
        return SowMilestonePosition.builder().sow(sow).milestone(milestone)
                .position(title).positionName("Developer").skill(skill)
                .seniority("Senior").locationType("Offshore").build();
    }
}
