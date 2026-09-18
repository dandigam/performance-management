package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashSet;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowMilestoneSummariesTest {
    @Mock SowRepository sowRepository;
    @Mock SowMilestoneRepository milestoneRepository;
    @Mock SowMilestonePositionAssignmentRepository positionAssignmentRepository;
    @InjectMocks SowServiceImpl service;

    @Test
    void pagesWithinSowAndCountsOnlyUnassignedOpenPositions() {
        Pageable pageable = PageRequest.of(1, 1, Sort.by("id"));
        var open = SowMilestonePosition.builder().id(1L).status("OPEN").build();
        var assigned = SowMilestonePosition.builder().id(2L).status("OPEN").build();
        var completed = SowMilestonePosition.builder().id(3L).status("COMPLETED").build();
        var milestone = SowMilestone.builder().id(201L).milestoneName("Milestone 1")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 30))
                .positions(new LinkedHashSet<>(List.of(open, assigned, completed))).build();
        when(sowRepository.existsById(101L)).thenReturn(true);
        when(milestoneRepository.findBySow_Id(101L, pageable))
                .thenReturn(new PageImpl<>(List.of(milestone), pageable, 3));
        var assignment = SowMilestonePositionAssignment.builder().milestonePosition(assigned).build();
        when(positionAssignmentRepository.findByMilestonePosition_Milestone_IdAndStatusIgnoreCase(201L, "ASSIGNED"))
                .thenReturn(List.of(assignment, assignment));

        var result = service.getMilestoneSummaries(101L, 1, 1);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        assertThat(result.content()).hasSize(1);
        var summary = result.content().get(0);
        assertThat(summary.milestoneId()).isEqualTo(201L);
        assertThat(summary.milestoneName()).isEqualTo("Milestone 1");
        assertThat(summary.startDate()).isEqualTo(milestone.getStartDate());
        assertThat(summary.endDate()).isEqualTo(milestone.getEndDate());
        assertThat(summary.totalPositionCount()).isEqualTo(3);
        assertThat(summary.openPositionCount()).isEqualTo(1);
    }

    @Test
    void emptyPageDoesNotFetchAssignments() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(sowRepository.existsById(101L)).thenReturn(true);
        when(milestoneRepository.findBySow_Id(101L, pageable)).thenReturn(Page.empty(pageable));
        var result = service.getMilestoneSummaries(101L, 0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        verifyNoInteractions(positionAssignmentRepository);
    }

    @Test
    void missingSowReturnsNotFound() {
        assertThatThrownBy(() -> service.getMilestoneSummaries(999L, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(milestoneRepository, positionAssignmentRepository);
    }

    @Test
    void invalidPaginationIsRejectedBeforeQueries() {
        assertThatThrownBy(() -> service.getMilestoneSummaries(101L, -1, 20))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getMilestoneSummaries(101L, 0, 0))
                .isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(sowRepository, milestoneRepository, positionAssignmentRepository);
    }
}
