package com.rit.performance.service;

import com.rit.performance.entity.Holiday;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HolidayServiceTest {
    private final HolidayRepository repository = mock(HolidayRepository.class);
    private final HolidayService service = new HolidayService(repository);

    @Test void permanentlyDeletesActiveAndPreviouslyDisabledHolidays() {
        for (boolean active : new boolean[]{true, false}) {
            Holiday holiday = new Holiday();
            holiday.setId(active ? 1L : 2L);
            holiday.setActive(active);
            when(repository.findById(holiday.getId())).thenReturn(Optional.of(holiday));
            service.delete(holiday.getId());
            verify(repository).delete(holiday);
            verify(repository, never()).save(holiday);
        }
        verify(repository, times(2)).flush();
    }

    @Test void preservesHolidayReferencedByTimesheetEntry() {
        Holiday holiday = new Holiday();
        when(repository.findById(1L)).thenReturn(Optional.of(holiday));
        when(repository.isReferencedByEntries(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("timesheet records");
        verify(repository, never()).delete(any(Holiday.class));
    }

    @Test void preservesHolidayReferencedByScheduledDay() {
        Holiday holiday = new Holiday();
        when(repository.findById(1L)).thenReturn(Optional.of(holiday));
        when(repository.isReferencedByScheduledDays(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("timesheet records");
        verify(repository, never()).delete(any(Holiday.class));
    }

    @Test void missingHolidayReturnsNotFound() {
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(Holiday.class));
    }
}
