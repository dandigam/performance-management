package com.rit.performance.service;

import com.rit.performance.dto.response.SowFeatureResponse;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowFeature;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.mapper.SowFeatureMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SowFeatureMapperTest {

    @Test
    void includesMilestoneAndDescription() {
        Sow sow = Sow.builder().id(8L).build();
        SowMilestone milestone = SowMilestone.builder()
                .id(21L)
                .milestoneName("UI Changes")
                .startDate(LocalDate.of(2026, 7, 28))
                .endDate(LocalDate.of(2026, 7, 30))
                .build();
        SowFeature feature = SowFeature.builder()
                .id(31L)
                .sow(sow)
                .milestone(milestone)
                .featureCode("F001")
                .featureName("Customer Login")
                .description("Implement the customer login screen")
                .startDate(LocalDate.of(2026, 7, 28))
                .endDate(LocalDate.of(2026, 7, 30))
                .status("TODO")
                .build();

        SowFeatureResponse response = SowFeatureMapper.toResponse(feature);

        assertEquals(21L, response.getMilestoneId());
        assertEquals("UI Changes", response.getMilestoneName());
        assertEquals(LocalDate.of(2026, 7, 28), response.getMilestoneStartDate());
        assertEquals(LocalDate.of(2026, 7, 30), response.getMilestoneEndDate());
        assertEquals("Implement the customer login screen", response.getDescription());
        assertEquals("F001", response.getFeatureCode());
    }
}
