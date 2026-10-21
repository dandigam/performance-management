package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowResourceRequirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowMilestoneDeleteTest {
    @Mock SowRepository sows;
    @Mock SowMilestoneRepository milestones;
    @Mock SowMilestonePositionAssignmentRepository assignments;
    @Mock SowFeatureRepository features;
    @Mock SowInvoiceRepository invoices;
    @Mock TimesheetEmployeeProjectRepository projects;
    @Mock TimesheetEmployeeProjectDayRepository days;
    @Mock SowResourceRequirementService requirements;
    @InjectMocks SowServiceImpl service;
    Sow sow;
    SowMilestone milestone;

    @BeforeEach void setup() {
        sow = new Sow();
        sow.setId(20L);
        milestone = new SowMilestone();
        milestone.setId(6L);
        sow.addMilestone(milestone);
    }

    private void found() {
        when(milestones.findByIdAndSow_Id(6L, 20L)).thenReturn(Optional.of(milestone));
    }

    @Test void deletesEmptyMilestoneAndRebuildsRequirements() {
        found();
        service.deleteMilestone(20L, 6L);
        assertTrue(sow.getMilestones().isEmpty());
        var order = inOrder(assignments, sows, requirements);
        order.verify(assignments).deleteByMilestonePosition_Milestone_Id(6L);
        order.verify(assignments).flush();
        order.verify(sows).saveAndFlush(sow);
        order.verify(requirements).onPositionRemoved(20L);
    }

    @Test void deletesMilestoneWithUnassignedPositions() {
        found();
        milestone.addPosition(SowMilestonePosition.builder().id(7L).status("OPEN").build());
        service.deleteMilestone(20L, 6L);
        verify(assignments).deleteByMilestonePosition_Milestone_Id(6L);
        verify(sows).saveAndFlush(sow);
    }

    @Test void oneAssignedPositionBlocksEntireDeletion() {
        found();
        milestone.addPosition(SowMilestonePosition.builder().id(7L).status("OPEN").build());
        milestone.addPosition(SowMilestonePosition.builder().id(8L).status("ASSIGNED").build());
        assertBlocked("MILESTONE_HAS_ASSIGNED_POSITIONS");
    }

    @Test void activeAssignmentBlocksEvenWhenPositionStatusIsOpen() {
        found();
        when(assignments.findByMilestonePosition_Milestone_IdAndStatusIgnoreCase(6L, "ASSIGNED"))
                .thenReturn(List.of(new SowMilestonePositionAssignment()));
        var error = assertBlocked("MILESTONE_HAS_ASSIGNED_POSITIONS");
        assertEquals("Milestone cannot be deleted. Please unassign all assigned positions first.",
                error.getMessage());
    }

    @Test void wrongSowOrMissingMilestoneReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.deleteMilestone(20L, 6L));
        verifyNoInteractions(assignments, sows, requirements);
    }

    @Test void linkedFeatureBlocksDeletion() {
        found();
        when(features.existsByMilestone_Id(6L)).thenReturn(true);
        assertBlocked("MILESTONE_HAS_FEATURES");
    }

    @Test void linkedInvoiceBlocksDeletion() {
        found();
        when(invoices.existsByMilestone_Id(6L)).thenReturn(true);
        assertBlocked("MILESTONE_HAS_INVOICE");
    }

    @Test void linkedTimesheetSetupBlocksDeletion() {
        found();
        when(projects.existsByMilestone_IdOrMilestonePositionAssignment_MilestonePosition_Milestone_Id(
                6L, 6L)).thenReturn(true);
        assertBlocked("MILESTONE_HAS_TIMESHEETS");
    }

    @Test void linkedTimesheetDayBlocksDeletion() {
        found();
        when(days.existsByMilestone_Id(6L)).thenReturn(true);
        assertBlocked("MILESTONE_HAS_TIMESHEETS");
    }

    private InvalidOperationException assertBlocked(String code) {
        var error = assertThrows(InvalidOperationException.class,
                () -> service.deleteMilestone(20L, 6L));
        assertEquals(code, error.getCode());
        assertTrue(sow.getMilestones().contains(milestone));
        verify(assignments, never()).deleteByMilestonePosition_Milestone_Id(anyLong());
        verifyNoInteractions(sows, requirements);
        return error;
    }
}
