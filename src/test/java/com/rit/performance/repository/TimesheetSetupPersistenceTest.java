package com.rit.performance.repository;

import com.rit.performance.entity.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class TimesheetSetupPersistenceTest extends EmployeeSummaryQueryTest {
    @Test void permitsReturnAssignmentSetupAndInternalWorkWithoutSow() {
        var employee = employee("Worker");
        var approver1 = employee("Approver1");
        var approver2 = employee("Approver2");
        var type = LookupType.builder().code("SETUP").name("Setup").build(); em.persist(type);
        var lookup = LookupValue.builder().lookupType(type).code("ACTIVE").name("Active").build(); em.persist(lookup);
        var sow = Sow.builder().sowName("Project").sowType("TEST").engagementType("TEST").status(lookup).build(); em.persist(sow);
        var milestone = SowMilestone.builder().sow(sow).milestoneName("M1").startDate(today.minusYears(1)).endDate(today.plusYears(1)).build(); em.persist(milestone);
        var position = SowMilestonePosition.builder().sow(sow).milestone(milestone).position(lookup).seniority(lookup).positionName("Developer").build(); em.persist(position);
        var parent = new EmployeeAssignment(); parent.setEmployeeId(employee.getId()); parent.setSowId(sow.getId()); parent.setEffectiveFrom(today.minusMonths(3)); em.persist(parent);
        var old = SowMilestonePositionAssignment.builder().employeeAssignment(parent).milestonePosition(position)
                .assignmentStartDate(today.minusMonths(3)).assignmentEndDate(today.minusMonths(2)).positionType("BILLABLE").status("COMPLETED").build(); em.persist(old);
        var current = SowMilestonePositionAssignment.builder().employeeAssignment(parent).milestonePosition(position)
                .assignmentStartDate(today).positionType("BILLABLE").status("ASSIGNED").build(); em.persist(current);
        var first = setup(employee, approver1, approver2);
        first.setSow(sow); first.setMilestone(milestone); first.setMilestonePositionAssignment(old);
        first.setAssignmentStartDate(old.getAssignmentStartDate()); first.setAssignmentEndDate(old.getAssignmentEndDate());
        first.setStatus(TimesheetEmployeeProjectStatus.COMPLETED); em.persist(first);
        var second = setup(employee, approver1, approver2);
        second.setSow(sow); second.setMilestone(milestone); second.setMilestonePositionAssignment(current);
        second.setAssignmentStartDate(today); em.persist(second);
        var internal = setup(employee, approver1, approver2);
        internal.setWorkType(TimesheetWorkType.INTERNAL); internal.setInternalWorkType("TRAINING"); em.persist(internal);
        em.flush(); em.clear();
        assertThat(em.find(TimesheetEmployeeProject.class, second.getId()).getAssignmentEndDate()).isNull();
        assertThat(em.find(TimesheetEmployeeProject.class, first.getId()).getEndDate()).isEqualTo(today.plusMonths(2));
        assertThat(em.find(TimesheetEmployeeProject.class, internal.getId()).getSow()).isNull();
    }
    private Employee employee(String name) {
        var employee = new Employee(); employee.setFirstName(name); employee.setEmail(name+"@test.com"); employee.setJoiningDate(today.minusYears(1)); em.persist(employee); return employee;
    }
    private TimesheetEmployeeProject setup(Employee employee, Employee first, Employee second) {
        var setup = new TimesheetEmployeeProject(); setup.setEmployee(employee); setup.setLevel1Approver(first); setup.setLevel2Approver(second);
        setup.setStartDate(today.minusMonths(3)); setup.setEndDate(today.plusMonths(2));
        setup.setCreatedOn(LocalDateTime.now()); setup.setUpdatedOn(LocalDateTime.now()); return setup;
    }
}
