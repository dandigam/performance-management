package com.rit.performance.repository;

import com.rit.performance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.*;

class AssignmentOptionsQueryTest extends EmployeeSummaryQueryTest {
    @Test void onlyReturnsEligibleOpenPositionsWithoutAssignedResources() {
        var type = LookupType.builder().code("OPTIONS").name("Options").build(); em.persist(type);
        var active = LookupValue.builder().lookupType(type).code("ACTIVE").name("Active").build(); em.persist(active);
        var draft = LookupValue.builder().lookupType(type).code("DRAFT").name("Draft").build(); em.persist(draft);
        var hold = LookupValue.builder().lookupType(type).code("ON_HOLD").name("On hold").build(); em.persist(hold);
        var sow = Sow.builder().sowName("CBMS").sowType("TEST").engagementType("TEST").status(active).build(); em.persist(sow);
        var milestone = SowMilestone.builder().sow(sow).milestoneName("Milestone")
                .startDate(today.minusDays(1)).endDate(today.plusDays(30)).build(); em.persist(milestone);
        var open = position(sow, milestone, active, "OPEN");
        var occupied = position(sow, milestone, active, "OPEN");
        position(sow, milestone, active, "CLOSED");
        var expired = position(sow, milestone, active, "OPEN"); expired.setEndDate(today.minusDays(1));
        var parent = new EmployeeAssignment(); parent.setEmployeeId(1L); parent.setSowId(sow.getId());
        parent.setEffectiveFrom(today); parent.setStatus("ASSIGNED"); em.persist(parent);
        var assignment = SowMilestonePositionAssignment.builder().milestonePosition(occupied).employeeAssignment(parent)
                .assignmentStartDate(today).positionType("BILLABLE").status("ASSIGNED").build(); em.persist(assignment);
        var repo = new JpaRepositoryFactory(em).getRepository(SowMilestonePositionRepository.class);
        assertThat(repo.findAssignmentOptions(today)).containsExactly(open);
        sow.setStatus(draft);
        assertThat(repo.findAssignmentOptions(today)).containsExactly(open);
        sow.setStatus(hold);
        assertThat(repo.findAssignmentOptions(today)).isEmpty();
        sow.setStatus(active); milestone.setEndDate(today.minusDays(1));
        assertThat(repo.findAssignmentOptions(today)).isEmpty();
    }

    private SowMilestonePosition position(Sow sow, SowMilestone milestone, LookupValue lookup, String status) {
        var p = SowMilestonePosition.builder().sow(sow).milestone(milestone).position(lookup).seniority(lookup)
                .positionName("Technical Lead").status(status).positionType("BILLABLE").build();
        em.persist(p); return p;
    }
}
