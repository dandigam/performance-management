package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowAssignmentUnassignRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowResourceRequirementService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SowMilestonePositionUnassignTest {
    private final SowMilestonePositionAssignmentRepository assignments = mock(SowMilestonePositionAssignmentRepository.class);
    private final SowMilestonePositionRepository positions = mock(SowMilestonePositionRepository.class);
    private final EmployeeAssignmentRepository employeeAssignments = mock(EmployeeAssignmentRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final SowResourceRequirementService requirements = mock(SowResourceRequirementService.class);
    private final TimesheetEmployeeProjectRepository setups = mock(TimesheetEmployeeProjectRepository.class);
    private final TimesheetAssignmentCompletionService completion = mock(TimesheetAssignmentCompletionService.class);
    private final SowMilestonePositionAssignmentServiceImpl service = new SowMilestonePositionAssignmentServiceImpl(
            assignments, positions, employeeAssignments, employees, requirements, setups, completion);

    @Test void unassignReopensPositionEvenWhenMilestoneHasEnded() {
        SowMilestonePositionAssignment assignment = stubAssignment();
        var response = service.unassign(20L, 3L, request());
        assertEquals("UNASSIGNED", response.getStatus());
        assertEquals("OPEN", assignment.getMilestonePosition().getStatus());
        verify(positions).saveAndFlush(assignment.getMilestonePosition());
    }

    @Test void unassignKeepsPositionFilledWhenAnotherActiveAssignmentRemains() {
        SowMilestonePositionAssignment assignment = stubAssignment();
        when(assignments.existsByMilestonePosition_IdAndStatusIgnoreCase(7L, "ASSIGNED"))
                .thenReturn(true);
        service.unassign(20L, 3L, request());
        assertEquals("ASSIGNED", assignment.getMilestonePosition().getStatus());
    }

    private SowAssignmentUnassignRequest request() {
        return SowAssignmentUnassignRequest.builder()
                .assignmentEndDate(LocalDate.of(2026, 10, 1))
                .assignmentStatus("UNASSIGNED")
                .updatedBy(3L).build();
    }

    private SowMilestonePositionAssignment stubAssignment() {
        Sow sow = new Sow(); sow.setId(20L);
        SowMilestone milestone = new SowMilestone(); milestone.setId(6L);
        milestone.setEndDate(LocalDate.of(2026, 9, 30));
        LookupValue positionType = new LookupValue(); positionType.setId(21L);
        SowMilestonePosition position = new SowMilestonePosition(); position.setId(7L);
        position.setSow(sow); position.setMilestone(milestone);
        position.setPosition(positionType); position.setStatus("ASSIGNED");
        EmployeeAssignment parent = new EmployeeAssignment(); parent.setId(8L);
        parent.setEmployeeId(5L); parent.setSowId(20L);
        SowMilestonePositionAssignment assignment = new SowMilestonePositionAssignment();
        assignment.setId(3L); assignment.setMilestonePosition(position);
        assignment.setEmployeeAssignment(parent); assignment.setStatus("ASSIGNED");
        assignment.setAssignmentStartDate(LocalDate.of(2026, 9, 1));
        when(assignments.findOneById(3L)).thenReturn(Optional.of(assignment));
        when(assignments.saveAndFlush(assignment)).thenReturn(assignment);
        when(assignments.findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(5L))
                .thenReturn(List.of(assignment));
        when(assignments.findByEmployeeAssignment_IdOrderByAssignmentStartDateDescIdDesc(8L))
                .thenReturn(List.of(assignment));
        return assignment;
    }
}
