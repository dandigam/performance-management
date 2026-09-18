package com.rit.performance.service.impl;

import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowSummariesTest {
    @Mock SowRepository sowRepository;
    @Mock CsxEmployeeRepository csxEmployeeRepository;
    @Mock SowMilestonePositionRepository positionRepository;
    @Mock SowMilestonePositionAssignmentRepository positionAssignmentRepository;
    @InjectMocks SowServiceImpl service;

    @Test
    void countsPositionsAndExcludesActiveAssignmentsAndCompletedPositions() {
        CsxEmployee poc = new CsxEmployee();
        poc.setId(25L);
        poc.setFirstName("Kalyan");
        poc.setLastName("Kandlakunta");
        Employee ritContact = new Employee();
        ritContact.setId(5L);
        ritContact.setFirstName("Srini");
        Sow sow = Sow.builder().id(101L).sowName("CBMS Support")
                .ritContactEmployee(ritContact).csxContactEmployeeId(25L)
                .businessUnit(LookupValue.builder().id(10L).name("Car Management").build())
                .status(LookupValue.builder().code("ACTIVE").build()).build();
        Pageable pageable = PageRequest.of(1, 1, Sort.by("id"));
        when(sowRepository.findSummaryPage(pageable))
                .thenReturn(new PageImpl<>(List.of(sow), pageable, 3));
        when(csxEmployeeRepository.findAllById(java.util.Set.of(25L))).thenReturn(List.of(poc));
        SowMilestonePosition open = SowMilestonePosition.builder().id(1L).status("OPEN").build();
        SowMilestonePosition assigned = SowMilestonePosition.builder().id(2L).status("OPEN").build();
        SowMilestonePosition completed = SowMilestonePosition.builder().id(3L).status("COMPLETED").build();
        when(positionRepository.findBySowId(101L)).thenReturn(List.of(open, assigned, completed));
        var assignment = SowMilestonePositionAssignment.builder().milestonePosition(assigned).build();
        when(positionAssignmentRepository.findByMilestonePosition_Sow_IdAndStatusIgnoreCase(101L, "ASSIGNED"))
                .thenReturn(List.of(assignment, assignment));

        var result = service.getSummaries(1, 1);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        var summary = result.content().get(0);
        assertThat(summary.getSowId()).isEqualTo(101L);
        assertThat(summary.getPocEmployeeName()).isEqualTo("Kalyan Kandlakunta");
        assertThat(summary.getPocEmployeeId()).isEqualTo(25L);
        assertThat(summary.getBusinessUnitName()).isEqualTo("Car Management");
        assertThat(summary.getStatus()).isEqualTo("ACTIVE");
        assertThat(summary.getTotalPositionCount()).isEqualTo(3);
        assertThat(summary.getOpenPositionCount()).isEqualTo(1);
        verify(sowRepository, never()).findAllWithDetails();
    }

    @Test
    void emptyPageSkipsPositionQueries() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(sowRepository.findSummaryPage(pageable)).thenReturn(Page.empty(pageable));
        var result = service.getSummaries(0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        verifyNoInteractions(positionRepository, positionAssignmentRepository);
    }

    @Test
    void missingCsxContactDoesNotFallBackToRitContact() {
        Employee rit = new Employee(); rit.setId(5L); rit.setFirstName("Srini");
        Sow sow = Sow.builder().id(20L).ritContactEmployee(rit).build();
        when(sowRepository.findSummaryPage(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sow)));
        var summary = service.getSummaries(0, 20).content().get(0);
        assertThat(summary.getPocEmployeeId()).isNull();
        assertThat(summary.getPocEmployeeName()).isNull();
        verifyNoInteractions(csxEmployeeRepository);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.getSummaries(-1, 20)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getSummaries(0, 0)).isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(sowRepository);
    }
}
