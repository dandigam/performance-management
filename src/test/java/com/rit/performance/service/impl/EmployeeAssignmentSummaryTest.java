package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.service.EmployeeAssignmentSummary;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeAssignmentSummaryTest {
    @Test
    void derivesResponseValuesFromSowAndActiveMilestoneAssignment() {
        Sow sow = new Sow();
        LookupValue department = new LookupValue();
        department.setId(48L);
        sow.setBusinessUnit(department);
        var active = detail(20L, "ASSIGNED");
        var inactive = detail(21L, "INACTIVE");
        var summary = EmployeeAssignmentSummary.from(sow, List.of(active, inactive));
        assertThat(summary.getDepartmentId()).isEqualTo(48L);
        assertThat(summary.getDesignationId()).isEqualTo(20L);
        assertThat(summary.getMilestoneId()).isEqualTo(88L);
        assertThat(summary.getPositionType()).isEqualTo("BILLABLE");
    }

    @Test
    void conflictingChildDesignationsDoNotChooseAnArbitraryPosition() {
        var summary = EmployeeAssignmentSummary.from(null,
                List.of(detail(20L, "ASSIGNED"), detail(21L, "ASSIGNED")));
        assertThat(summary.getDesignationId()).isNull();
        assertThat(summary.getMilestoneId()).isEqualTo(88L);
        assertThat(summary.getDepartmentId()).isNull();
    }

    private SowMilestonePositionAssignment detail(Long designationId, String status) {
        LookupValue designation = new LookupValue();
        designation.setId(designationId);
        SowMilestone milestone = new SowMilestone();
        milestone.setId(88L);
        SowMilestonePosition position = new SowMilestonePosition();
        position.setPosition(designation);
        position.setMilestone(milestone);
        SowMilestonePositionAssignment detail = new SowMilestonePositionAssignment();
        detail.setMilestonePosition(position);
        detail.setStatus(status);
        detail.setPositionType("BILLABLE");
        return detail;
    }
}
