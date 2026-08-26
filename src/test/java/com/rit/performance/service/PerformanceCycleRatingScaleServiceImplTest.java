package com.rit.performance.service;

import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.PerformanceCycleRatingScaleRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerformanceCycleRatingScaleServiceImplTest {

    @Test
    void findByCycleIdReturnsEmptyWhenRatingScaleIsNotConfigured() {
        PerformanceCycleRatingScaleRepository repository =
                mock(PerformanceCycleRatingScaleRepository.class);
        PerformanceCycleRatingScaleServiceImpl service = new PerformanceCycleRatingScaleServiceImpl(
                repository,
                mock(PerformanceCycleConfigRepository.class),
                mock(LookupValueRepository.class));

        when(repository.findByPerformanceCycleId(9L)).thenReturn(Optional.empty());

        assertTrue(service.findByCycleId(9L).isEmpty());
    }
}
