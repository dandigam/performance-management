package com.rit.performance.service;

import com.rit.performance.dto.request.SowFeatureRequest;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowFeature;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.SowFeatureRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.service.impl.SowFeatureServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SowFeatureServiceImplTest {

    @Test
    void createGeneratesCodeAndAssociatesMilestone() {
        SowRepository sowRepository = mock(SowRepository.class);
        SowFeatureRepository featureRepository = mock(SowFeatureRepository.class);
        SowMilestoneRepository milestoneRepository = mock(SowMilestoneRepository.class);
        SowFeatureServiceImpl service = new SowFeatureServiceImpl(
                sowRepository, featureRepository, milestoneRepository);
        Sow sow = Sow.builder().id(8L).build();
        SowMilestone milestone = milestone(21L, sow);
        SowFeature existing = SowFeature.builder().featureCode("F002").build();
        when(sowRepository.findById(8L)).thenReturn(Optional.of(sow));
        when(featureRepository.findBySow_IdOrderByIdAsc(8L))
                .thenReturn(List.of(existing));
        when(milestoneRepository.findByIdAndSow_Id(21L, 8L)).thenReturn(Optional.of(milestone));
        when(featureRepository.save(any(SowFeature.class))).thenAnswer(invocation -> {
            SowFeature feature = invocation.getArgument(0);
            feature.setId(31L);
            return feature;
        });

        var response = service.create(8L, request());

        ArgumentCaptor<SowFeature> captor = ArgumentCaptor.forClass(SowFeature.class);
        verify(featureRepository).save(captor.capture());
        assertEquals("F003", captor.getValue().getFeatureCode());
        assertEquals("TODO", captor.getValue().getStatus());
        assertEquals(21L, response.getMilestoneId());
        assertEquals("Customer Login", response.getFeatureName());
        assertEquals("Login experience", response.getDescription());
    }

    @Test
    void createRejectsDatesOutsideMilestone() {
        SowRepository sowRepository = mock(SowRepository.class);
        SowFeatureRepository featureRepository = mock(SowFeatureRepository.class);
        SowMilestoneRepository milestoneRepository = mock(SowMilestoneRepository.class);
        SowFeatureServiceImpl service = new SowFeatureServiceImpl(
                sowRepository, featureRepository, milestoneRepository);
        Sow sow = Sow.builder().id(8L).build();
        when(sowRepository.findById(8L)).thenReturn(Optional.of(sow));
        when(featureRepository.findBySow_IdOrderByIdAsc(8L))
                .thenReturn(List.of());
        when(milestoneRepository.findByIdAndSow_Id(21L, 8L))
                .thenReturn(Optional.of(milestone(21L, sow)));
        SowFeatureRequest request = request();
        request.setEndDate(LocalDate.of(2026, 7, 31));

        assertThrows(InvalidOperationException.class, () -> service.create(8L, request));
        verify(featureRepository, never()).save(any());
    }

    private SowFeatureRequest request() {
        return SowFeatureRequest.builder()
                .milestoneId(21L)
                .featureName("Customer Login")
                .description(" Login experience ")
                .startDate(LocalDate.of(2026, 7, 28))
                .endDate(LocalDate.of(2026, 7, 30))
                .build();
    }

    private SowMilestone milestone(Long id, Sow sow) {
        return SowMilestone.builder()
                .id(id)
                .sow(sow)
                .milestoneName("UI Changes")
                .startDate(LocalDate.of(2026, 7, 28))
                .endDate(LocalDate.of(2026, 7, 30))
                .build();
    }
}
