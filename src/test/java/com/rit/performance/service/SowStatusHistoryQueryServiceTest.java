package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class SowStatusHistoryQueryServiceTest {
    private final SowRepository sows = mock(SowRepository.class);
    private final SowStatusHistoryRepository history = mock(SowStatusHistoryRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SowStatusHistoryQueryService service = new SowStatusHistoryQueryService(sows, history, users);

    @Test void returnsBaselineAndTransitionWithActorName() {
        when(sows.existsById(2L)).thenReturn(true);
        var baseline = new SowStatusHistory(); baseline.setSowId(2L); baseline.setStatus("DRAFT"); baseline.setBaseline(true);
        var transition = new SowStatusHistory(); transition.setSowId(2L); transition.setPreviousStatus("DRAFT");
        transition.setStatus("WAITING_FOR_APPROVAL"); transition.setChangedBy(7L);
        when(history.findBySowIdOrderByChangedAtAscIdAsc(2L)).thenReturn(List.of(baseline, transition));
        var employee = new Employee(); employee.setFirstName("Test"); employee.setLastName("User");
        var user = new User(); user.setId(7L); user.setEmployee(employee);
        when(users.findAllById(List.of(7L))).thenReturn(List.of(user));
        var result = service.list(2L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).baseline()).isTrue();
        assertThat(result.get(0).changedByName()).isNull();
        assertThat(result.get(0).statusEffectiveDate()).isNull();
        assertThat(result.get(1).changedByName()).isEqualTo("Test User");
        assertThat(result.get(1).previousStatus()).isEqualTo("DRAFT");
        verify(history).findBySowIdOrderByChangedAtAscIdAsc(2L);
    }
    @Test void existingSowWithoutHistoryReturnsEmptyList() {
        when(sows.existsById(2L)).thenReturn(true);
        when(history.findBySowIdOrderByChangedAtAscIdAsc(2L)).thenReturn(List.of());
        assertThat(service.list(2L)).isEmpty();
        verifyNoInteractions(users);
    }
    @Test void missingSowReturnsNotFound() {
        assertThatThrownBy(() -> service.list(99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(history, users);
    }
    @Test void unresolvedActorPreservesIdWithoutInventingName() {
        when(sows.existsById(2L)).thenReturn(true);
        var row = new SowStatusHistory(); row.setSowId(2L); row.setChangedBy(8L);
        when(history.findBySowIdOrderByChangedAtAscIdAsc(2L)).thenReturn(List.of(row));
        when(users.findAllById(List.of(8L))).thenReturn(List.of());
        var result = service.list(2L).get(0);
        assertThat(result.changedBy()).isEqualTo(8L);
        assertThat(result.changedByName()).isNull();
    }
}
