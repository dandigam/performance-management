package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.SowMilestonePositionRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;

class AssignmentOptionsServiceTest {
    @Test void nestsPositionsAndUsesMilestonePositionIdAndNumericHours() {
        var repo = mock(SowMilestonePositionRepository.class);
        var sow = Sow.builder().id(20L).sowName("CBMS").status(LookupValue.builder().code("ACTIVE").build()).build();
        var milestone = SowMilestone.builder().id(129L).milestoneName("Milestone 1").sow(sow).build();
        var position = SowMilestonePosition.builder().id(18L).sow(sow).milestone(milestone)
                .position(LookupValue.builder().id(21L).build()).positionName("Technical Lead").status("OPEN").hours("4096").build();
        when(repo.findAssignmentOptions(any())).thenReturn(List.of(position));
        var result = new AssignmentOptionsService(repo).getSows();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).sowId()).isEqualTo(20L);
        var option = result.get(0).milestones().get(0).positions().get(0);
        assertThat(option.positionId()).isEqualTo(18L);
        assertThat(option.designationId()).isEqualTo(21L);
        assertThat(option.status()).isEqualTo("OPEN");
        assertThat(option.hours()).isEqualByComparingTo("4096");
        position.setHours("To be estimated");
        assertThat(new AssignmentOptionsService(repo).getSows().get(0).milestones().get(0).positions().get(0).hours()).isNull();
        when(repo.findAssignmentOptions(any())).thenReturn(List.of());
        assertThat(new AssignmentOptionsService(repo).getSows()).isEmpty();
    }
}
