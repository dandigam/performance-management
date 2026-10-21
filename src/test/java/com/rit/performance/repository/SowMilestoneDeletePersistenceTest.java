package com.rit.performance.repository;

import com.rit.performance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.*;

class SowMilestoneDeletePersistenceTest extends EmployeeSummaryQueryTest {
    @Test void removesInactiveAssignmentsBeforeMilestoneAndPositionOrphans() {
        var type = LookupType.builder().code("DELETE_TEST").name("Delete test").build();
        em.persist(type);
        var lookup = LookupValue.builder().lookupType(type).code("ACTIVE").name("Active").build();
        em.persist(lookup);
        var sow = Sow.builder().sowName("Delete test").sowType("TEST")
                .engagementType("TEST").status(lookup).build();
        var milestone = SowMilestone.builder().milestoneName("Phase 1")
                .startDate(today).endDate(today.plusDays(30)).build();
        sow.addMilestone(milestone);
        var position = SowMilestonePosition.builder().position(lookup).seniority(lookup)
                .positionName("Developer").status("OPEN").build();
        milestone.addPosition(position);
        em.persist(sow);
        var parent = new EmployeeAssignment();
        parent.setEmployeeId(123L);
        parent.setSowId(sow.getId());
        parent.setEffectiveFrom(today);
        parent.setStatus("COMPLETED");
        em.persist(parent);
        var assignment = SowMilestonePositionAssignment.builder()
                .employeeAssignment(parent).milestonePosition(position)
                .assignmentStartDate(today).assignmentEndDate(today)
                .positionType("BILLABLE").status("COMPLETED").build();
        em.persist(assignment);
        em.flush();

        var repositories = new JpaRepositoryFactory(em);
        var assignments = repositories.getRepository(SowMilestonePositionAssignmentRepository.class);
        var projects = repositories.getRepository(TimesheetEmployeeProjectRepository.class);
        var days = repositories.getRepository(TimesheetEmployeeProjectDayRepository.class);
        assertThat(assignments.findByMilestonePosition_Milestone_IdAndStatusIgnoreCase(
                milestone.getId(), "ASSIGNED")).isEmpty();
        assertThat(projects
                .existsByMilestone_IdOrMilestonePositionAssignment_MilestonePosition_Milestone_Id(
                        milestone.getId(), milestone.getId())).isFalse();
        assertThat(days.existsByMilestone_Id(milestone.getId())).isFalse();

        Long milestoneId = milestone.getId();
        Long positionId = position.getId();
        Long assignmentId = assignment.getId();
        assignments.deleteByMilestonePosition_Milestone_Id(milestoneId);
        assignments.flush();
        sow.removeMilestone(milestone);
        em.flush();
        em.clear();
        assertThat(em.find(SowMilestone.class, milestoneId)).isNull();
        assertThat(em.find(SowMilestonePosition.class, positionId)).isNull();
        assertThat(em.find(SowMilestonePositionAssignment.class, assignmentId)).isNull();
        assertThat(em.find(Sow.class, sow.getId())).isNotNull();
        assertThat(em.find(EmployeeAssignment.class, parent.getId())).isNotNull();
    }
}
