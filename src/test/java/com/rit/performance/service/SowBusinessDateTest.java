package com.rit.performance.service;

import com.rit.performance.dto.request.SowStatusUpdateRequest;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.service.impl.SowServiceImpl;
import java.time.*;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SowBusinessDateTest {
    @Test void submissionIgnoresIndiaDateAndAcceptsOmittedDate() throws Exception {
        var constructor = SowServiceImpl.class.getConstructors()[0];
        var service = (SowServiceImpl) constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                .map(type -> mock(type)).toArray());
        var clock = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), ZoneId.of("America/New_York"));
        ReflectionTestUtils.setField(service, "businessClock", clock);
        var repository = (SowRepository) ReflectionTestUtils.getField(service, "sowRepository");
        var lookups = (LookupValueRepository) ReflectionTestUtils.getField(service, "lookupValueRepository");
        var draft = new LookupValue(); draft.setCode("DRAFT");
        var waiting = new LookupValue(); waiting.setCode("WAITING_FOR_APPROVAL");
        when(lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                "SOW_STATUS", "WAITING_FOR_APPROVAL")).thenReturn(Optional.of(waiting));
        for (LocalDate browserDate : Arrays.asList(LocalDate.of(2026, 10, 10), null)) {
            var sow = new Sow(); sow.setId(1L); sow.setStatus(draft);
            when(repository.findForStatusUpdate(1L)).thenReturn(Optional.of(sow));
            when(repository.save(sow)).thenReturn(sow);
            var response = service.updateStatus(1L, SowStatusUpdateRequest.builder()
                    .status("WAITING_FOR_APPROVAL").statusEffectiveDate(browserDate).build());
            assertThat(response.getSubmittedDate()).isEqualTo(LocalDate.of(2026, 10, 9));
            assertThat(response.getStatusEffectiveDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        }
        var approved = new LookupValue(); approved.setCode("APPROVED");
        when(lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                "SOW_STATUS", "APPROVED")).thenReturn(Optional.of(approved));
        assertThatThrownBy(() -> service.updateStatus(1L, SowStatusUpdateRequest.builder()
                .status("APPROVED").statusEffectiveDate(LocalDate.of(2026, 10, 10)).build()))
                .hasMessage("statusEffectiveDate cannot be in the future");
        assertThatThrownBy(() -> service.updateStatus(1L, SowStatusUpdateRequest.builder()
                .status("APPROVED").build()))
                .hasMessage("statusEffectiveDate is required");
    }
}
