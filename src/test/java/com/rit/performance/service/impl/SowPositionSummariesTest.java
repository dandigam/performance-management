package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowPositionSummariesTest {
    @Mock SowMilestoneRepository milestoneRepository;
    @Mock SowMilestonePositionRepository positionRepository;
    @Mock SowMilestonePositionAssignmentRepository positionAssignmentRepository;
    @Mock EmployeeRepository employeeRepository;
    @InjectMocks SowServiceImpl service;

    @Test
    void mapsActiveAssignmentAndPreservesPagination() {
        var pageable = PageRequest.of(1, 1, Sort.by("id"));
        var position = SowMilestonePosition.builder().id(301L).positionName("Senior Java Developer")
                .locationType("ONSITE").hours("1040").status("FILLED").positionType("BILLABLE").build();
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(new SowMilestone()));
        when(positionRepository.findBySow_IdAndMilestone_Id(101L, 201L, pageable))
                .thenReturn(new PageImpl<>(List.of(position), pageable, 3));
        var parent = new EmployeeAssignment();
        parent.setEmployeeId(25L);
        var active = SowMilestonePositionAssignment.builder().id(401L).status("ASSIGNED").employeeAssignment(parent).build();
        var completed = SowMilestonePositionAssignment.builder().id(402L).status("COMPLETED").build();
        when(positionAssignmentRepository.findByMilestonePosition_IdOrderByAssignmentStartDateDescIdDesc(301L))
                .thenReturn(List.of(completed, active));
        var employee = new Employee();
        employee.setFirstName("Kalyan");
        employee.setLastName("Kandlakunta");
        when(employeeRepository.findById(25L)).thenReturn(Optional.of(employee));
        var result = service.getPositionSummaries(101L, 201L, 1, 1);
        var summary = result.content().get(0);
        assertThat(summary.positionId()).isEqualTo(301L);
        assertThat(summary.positionTitle()).isEqualTo("Senior Java Developer");
        assertThat(summary.location()).isEqualTo("ONSITE");
        assertThat(summary.estimatedHours()).isEqualByComparingTo("1040");
        assertThat(summary.status()).isEqualTo("ASSIGNED");
        assertThat(summary.assignmentId()).isEqualTo(401L);
        assertThat(summary.employeeId()).isEqualTo(25L);
        assertThat(summary.employeeName()).isEqualTo("Kalyan Kandlakunta");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
    }

    @Test
    void openPositionIgnoresCompletedAssignmentAndFreeTextHours() {
        var pageable = PageRequest.of(0, 20, Sort.by("id"));
        var position = SowMilestonePosition.builder().id(301L).status("OPEN").hours("TBD").build();
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(new SowMilestone()));
        when(positionRepository.findBySow_IdAndMilestone_Id(101L, 201L, pageable))
                .thenReturn(new PageImpl<>(List.of(position), pageable, 1));
        when(positionAssignmentRepository.findByMilestonePosition_IdOrderByAssignmentStartDateDescIdDesc(301L))
                .thenReturn(List.of(SowMilestonePositionAssignment.builder().status("COMPLETED").build()));
        var summary = service.getPositionSummaries(101L, 201L, 0, 20).content().get(0);
        assertThat(summary.status()).isEqualTo("OPEN");
        assertThat(summary.assignmentId()).isNull();
        assertThat(summary.employeeId()).isNull();
        assertThat(summary.employeeName()).isNull();
        assertThat(summary.estimatedHours()).isNull();
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void emptyPageSkipsAssignmentLookups() {
        var pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(new SowMilestone()));
        when(positionRepository.findBySow_IdAndMilestone_Id(101L, 201L, pageable)).thenReturn(Page.empty(pageable));
        var result = service.getPositionSummaries(101L, 201L, 0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        verifyNoInteractions(positionAssignmentRepository, employeeRepository);
    }

    @Test
    void rejectsMilestoneOutsideSow() {
        assertThatThrownBy(() -> service.getPositionSummaries(101L, 999L, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(positionRepository, positionAssignmentRepository, employeeRepository);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.getPositionSummaries(101L, 201L, -1, 20))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getPositionSummaries(101L, 201L, 0, 0))
                .isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(milestoneRepository, positionRepository);
    }
}
