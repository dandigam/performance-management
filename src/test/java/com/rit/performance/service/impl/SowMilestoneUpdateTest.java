package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowMilestoneUpdateRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.SowMilestoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowMilestoneUpdateTest {
    @Mock SowMilestoneRepository milestoneRepository;
    @InjectMocks SowServiceImpl service;

    private SowMilestoneUpdateRequest request() {
        return SowMilestoneUpdateRequest.builder().milestoneName("Updated milestone")
                .description("Description").deliverables("Deliverables")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2027, 12, 31))
                .invoiceDate(LocalDate.of(2028, 1, 3)).status("Planning").build();
    }

    @Test void updatesOnlySelectedMilestoneAndAllowsInvoiceOverride() {
        var sow = Sow.builder().id(101L).build();
        var milestone = SowMilestone.builder().id(201L).estimatedHours(1040).build();
        var sibling = SowMilestone.builder().id(202L).milestoneName("Other").build();
        sow.addMilestone(milestone);
        sow.addMilestone(sibling);
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.saveAndFlush(milestone)).thenReturn(milestone);
        var request = request();
        request.setInvoiceAmount(new BigDecimal("122880"));
        var response = service.updateMilestone(101L, 201L, request);
        assertThat(response.getMilestoneName()).isEqualTo("Updated milestone");
        assertThat(response.getAmount()).isEqualByComparingTo("122880");
        assertThat(response.getStatus()).isEqualTo("PLANNING");
        assertThat(response.getInvoiceDate()).isEqualTo(request.getInvoiceDate());
        assertThat(response.getEstimatedHours()).isEqualTo(1040);
        assertThat(sow.getMilestones()).containsExactly(milestone, sibling);
        assertThat(sibling.getMilestoneName()).isEqualTo("Other");
    }

    @Test void omittedInvoiceAmountUsesPositionsWithoutChangingThem() {
        var milestone = SowMilestone.builder().id(201L).status("IN_PROGRESS").build();
        var position = SowMilestonePosition.builder().id(301L).amount(new BigDecimal("1200"))
                .position(LookupValue.builder().id(1L).build()).build();
        milestone.addPosition(position);
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.saveAndFlush(milestone)).thenReturn(milestone);
        var request = request();
        request.setStatus(null);
        var response = service.updateMilestone(101L, 201L, request);
        assertThat(response.getAmount()).isEqualByComparingTo("1200");
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(milestone.getPositions()).containsExactly(position);
        assertThat(position.getAmount()).isEqualByComparingTo("1200");
    }

    @Test void rejectsMilestoneOutsideSow() {
        assertThatThrownBy(() -> service.updateMilestone(101L, 999L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(milestoneRepository, never()).saveAndFlush(any());
    }

    @Test void rejectsReversedDatesBeforeChangingMilestone() {
        var milestone = SowMilestone.builder().id(201L).milestoneName("Original").build();
        when(milestoneRepository.findByIdAndSow_Id(201L, 101L)).thenReturn(Optional.of(milestone));
        var request = request();
        request.setEndDate(request.getStartDate().minusDays(1));
        assertThatThrownBy(() -> service.updateMilestone(101L, 201L, request))
                .isInstanceOf(InvalidOperationException.class);
        assertThat(milestone.getMilestoneName()).isEqualTo("Original");
        verify(milestoneRepository, never()).saveAndFlush(any());
    }
}
