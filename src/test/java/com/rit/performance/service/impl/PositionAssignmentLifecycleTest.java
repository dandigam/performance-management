package com.rit.performance.service.impl;

import com.rit.performance.dto.request.*;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowResourceRequirementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionAssignmentLifecycleTest {
    @Mock SowMilestonePositionAssignmentRepository repository;
    @Mock SowMilestonePositionRepository positionRepository;
    @Mock EmployeeAssignmentRepository employeeAssignmentRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock SowResourceRequirementService resourceRequirementService;
    @Mock TimesheetEmployeeProjectRepository timesheetProjectRepository;
    @Mock TimesheetAssignmentCompletionService timesheetCompletionService;
    @InjectMocks SowMilestonePositionAssignmentServiceImpl service;

    private SowMilestonePositionAssignment setup(int daysUntilEnd, String status) {
        var parent = new EmployeeAssignment();
        parent.setId(4L);
        parent.setEmployeeId(25L);
        parent.setSowId(1L);
        parent.setStatus("ASSIGNED");
        var position = SowMilestonePosition.builder().id(3L).status("ASSIGNED")
                .sow(Sow.builder().id(1L).build())
                .milestone(SowMilestone.builder().id(2L).endDate(LocalDate.now().plusDays(daysUntilEnd)).build())
                .position(LookupValue.builder().id(8L).build()).build();
        var assignment = SowMilestonePositionAssignment.builder().id(5L).status(status)
                .assignmentStartDate(LocalDate.now().minusDays(10))
                .milestonePosition(position).employeeAssignment(parent).build();
        when(repository.findOneById(5L)).thenReturn(Optional.of(assignment));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return assignment;
    }

    private void unassign() {
        service.unassign(1L, 5L, SowAssignmentUnassignRequest.builder()
                .assignmentEndDate(LocalDate.now()).assignmentStatus("COMPLETED").build());
    }

    @Test void legacyActiveAssignmentCannotBeUnassigned() {
        var position = SowMilestonePosition.builder().sow(Sow.builder().id(1L).build()).build();
        var assignment = SowMilestonePositionAssignment.builder().status("ACTIVE")
                .milestonePosition(position).build();
        when(repository.findOneById(5L)).thenReturn(Optional.of(assignment));
        assertThatThrownBy(this::unassign)
                .isInstanceOf(com.rit.performance.exception.InvalidOperationException.class)
                .hasMessage("Only an ASSIGNED assignment can be unassigned");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void unassignmentBeforeEndReopensPositionAndCompletesEmployeeRole() {
        var assignment = setup(5, "ASSIGNED");
        unassign();
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("OPEN");
        assertThat(assignment.getEmployeeAssignment().getStatus()).isEqualTo("COMPLETED");
        verify(resourceRequirementService).onResourceCompleted(1L);
    }

    @Test void unassignmentCompletesMatchingTimesheetSetup() {
        setup(5, "ASSIGNED");
        var timesheetSetup = new TimesheetEmployeeProject();
        timesheetSetup.setStartDate(LocalDate.now().minusDays(10));
        timesheetSetup.setEndDate(LocalDate.now().plusYears(1));
        timesheetSetup.setStatus(TimesheetEmployeeProjectStatus.ACTIVE);
        when(timesheetProjectRepository.findByMilestonePositionAssignment_Id(5L))
                .thenReturn(Optional.of(timesheetSetup));
        unassign();
        assertThat(timesheetSetup.getAssignmentEndDate()).isEqualTo(LocalDate.now());
        assertThat(timesheetSetup.getEndDate()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(timesheetSetup.getStatus()).isEqualTo(TimesheetEmployeeProjectStatus.COMPLETED);
        verify(timesheetProjectRepository).save(timesheetSetup);
        verify(timesheetProjectRepository, never()).delete(any());
    }

    @Test void anotherAssignedRoleInSameMilestoneKeepsSharedSetupOpen() {
        var assignment = setup(5, "ASSIGNED");
        var other = SowMilestonePositionAssignment.builder().id(6L).status("ASSIGNED")
                .employeeAssignment(assignment.getEmployeeAssignment())
                .milestonePosition(assignment.getMilestonePosition()).build();
        when(repository.findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(25L))
                .thenReturn(List.of(assignment, other));
        unassign();
        verify(timesheetProjectRepository, never()).save(any());
    }

    @Test void nestedUnassignAlsoCompletesSetup() {
        var assignment = setup(5, "ASSIGNED");
        when(positionRepository.findByIdAndMilestone_IdAndSow_Id(3L, 2L, 1L))
                .thenReturn(Optional.of(assignment.getMilestonePosition()));
        var project = new TimesheetEmployeeProject();
        project.setStartDate(LocalDate.now().minusDays(10));
        when(timesheetProjectRepository.findByMilestonePositionAssignment_Id(5L))
                .thenReturn(Optional.of(project));
        service.unassign(1L, 2L, 3L, 5L, SowMilestonePositionUnassignRequest.builder()
                .assignmentEndDate(LocalDate.now()).assignmentStatus("COMPLETED").build());
        assertThat(project.getStatus()).isEqualTo(TimesheetEmployeeProjectStatus.COMPLETED);
        assertThat(project.getAssignmentEndDate()).isEqualTo(LocalDate.now());
    }

    @Test void unassignmentOnEndDateClosesPosition() {
        var assignment = setup(0, "ASSIGNED");
        unassign();
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("CLOSED");
    }

    @Test void unassignmentAfterEndDateClosesPosition() {
        var assignment = setup(-1, "ASSIGNED");
        unassign();
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("CLOSED");
    }

    @Test void otherAssignedEmployeeKeepsPositionAssigned() {
        var assignment = setup(-1, "ASSIGNED");
        when(repository.existsByMilestonePosition_IdAndStatusIgnoreCase(3L, "ASSIGNED")).thenReturn(true);
        when(repository.existsByEmployeeAssignment_IdAndStatusIgnoreCase(4L, "ASSIGNED")).thenReturn(true);
        unassign();
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("ASSIGNED");
        assertThat(assignment.getEmployeeAssignment().getStatus()).isEqualTo("ASSIGNED");
    }

    @Test void nestedUnassignmentUsesSameLifecycleAndNormalizesLegacyTerminalStatus() {
        var assignment = setup(5, "ASSIGNED");
        when(positionRepository.findByIdAndMilestone_IdAndSow_Id(3L, 2L, 1L))
                .thenReturn(Optional.of(assignment.getMilestonePosition()));
        service.unassign(1L, 2L, 3L, 5L, SowMilestonePositionUnassignRequest.builder()
                .assignmentEndDate(LocalDate.now()).assignmentStatus("UNASSIGNED").build());
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("OPEN");
    }

    @Test void updatingAssignmentToCompletedReopensPosition() {
        var assignment = setup(5, "ASSIGNED");
        when(positionRepository.findByIdAndMilestone_IdAndSow_Id(3L, 2L, 1L))
                .thenReturn(Optional.of(assignment.getMilestonePosition()));
        when(employeeAssignmentRepository.findById(4L)).thenReturn(Optional.of(assignment.getEmployeeAssignment()));
        service.update(1L, 2L, 3L, 5L, SowMilestonePositionAssignmentRequest.builder()
                .employeeAssignmentId(4L).positionType("BILLABLE").status("COMPLETED")
                .assignmentStartDate(LocalDate.now().minusDays(10)).assignmentEndDate(LocalDate.now()).build());
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");
        assertThat(assignment.getMilestonePosition().getStatus()).isEqualTo("OPEN");
    }

    @Test void creatingAssignedRoleMarksPositionAssigned() {
        var parent = new EmployeeAssignment();
        parent.setId(4L);
        parent.setEmployeeId(25L);
        parent.setSowId(1L);
        parent.setStatus("ASSIGNED");
        var position = SowMilestonePosition.builder().id(3L).status("OPEN")
                .sow(Sow.builder().id(1L).build())
                .milestone(SowMilestone.builder().id(2L).endDate(LocalDate.now().plusDays(5)).build())
                .position(LookupValue.builder().id(8L).build()).build();
        when(positionRepository.findByIdAndMilestone_IdAndSow_Id(3L, 2L, 1L)).thenReturn(Optional.of(position));
        when(employeeAssignmentRepository.findById(4L)).thenReturn(Optional.of(parent));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsByMilestonePosition_IdAndStatusIgnoreCase(3L, "ASSIGNED")).thenReturn(true);
        var response = service.create(1L, 2L, 3L, SowMilestonePositionAssignmentRequest.builder()
                .employeeAssignmentId(4L).positionType("BILLABLE").status("ASSIGNED")
                .assignmentStartDate(LocalDate.now()).build());
        assertThat(response.getStatus()).isEqualTo("ASSIGNED");
        assertThat(position.getStatus()).isEqualTo("ASSIGNED");
    }
}
