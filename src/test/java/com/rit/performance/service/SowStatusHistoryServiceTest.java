package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.SowStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SowStatusHistoryServiceTest {
    @Test void recordsFourStageLifecycleWithAuthenticatedActor() {
        var repository = mock(SowStatusHistoryRepository.class);
        var service = new SowStatusHistoryService(repository, () -> Optional.of(7L));
        Sow sow = new Sow();
        sow.setId(25L);
        sow.setStatusEffectiveDate(LocalDate.of(2026,10,3));
        String previous = null;
        for (String code : new String[]{"DRAFT","WAITING_FOR_APPROVAL","ACTIVE","COMPLETED"}) {
            LookupValue status = new LookupValue(); status.setCode(code); sow.setStatus(status);
            service.record(sow, previous); previous = code;
        }
        var captured = ArgumentCaptor.forClass(SowStatusHistory.class);
        verify(repository,times(4)).save(captured.capture());
        assertThat(captured.getAllValues()).extracting(SowStatusHistory::getPreviousStatus)
                .containsExactly(null,"DRAFT","WAITING_FOR_APPROVAL","ACTIVE");
        assertThat(captured.getAllValues()).allSatisfy(h -> {
            assertThat(h.getSowId()).isEqualTo(25L);
            assertThat(h.getChangedBy()).isEqualTo(7L);
            assertThat(h.getChangedAt()).isNotNull();
            assertThat(h.getApprovedAt()).isNull();
            assertThat(h.getStatusEffectiveDate()).isEqualTo(sow.getStatusEffectiveDate());
        });
    }
    @Test void explicitApprovalRecordsActualApprovalTimeAndNoInventedActor() {
        var repository = mock(SowStatusHistoryRepository.class);
        var service = new SowStatusHistoryService(repository, Optional::empty);
        Sow sow = new Sow(); sow.setId(1L);
        LookupValue status = new LookupValue(); status.setCode("APPROVED"); sow.setStatus(status);
        sow.setStatusEffectiveDate(LocalDate.of(2026,1,1));
        service.record(sow,"WAITING_FOR_APPROVAL");
        var captured = ArgumentCaptor.forClass(SowStatusHistory.class);
        verify(repository).save(captured.capture());
        assertThat(captured.getValue().getApprovedAt()).isEqualTo(captured.getValue().getChangedAt());
        assertThat(captured.getValue().getChangedBy()).isNull();
        service.record(sow,"APPROVED");
        verifyNoMoreInteractions(repository);
    }
}
