package com.rit.performance.controller;

import com.rit.performance.dto.SowResourceRequirementResponse;
import com.rit.performance.dto.SowResourceRequirementSummaryResponse;
import com.rit.performance.service.SowResourceRequirementService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SowResourceRequirementControllerTest {

    @Test
    void getsAllResourceRequirements() {
        SowResourceRequirementService service = mock(SowResourceRequirementService.class);
        List<SowResourceRequirementResponse> requirements = List.of(
                SowResourceRequirementResponse.builder().id(1L).sowId(10L).build());
        when(service.getAll()).thenReturn(requirements);

        ResponseEntity<List<SowResourceRequirementResponse>> response =
                new SowResourceRequirementController(service).getAll();

        assertSame(requirements, response.getBody());
        verify(service).getAll();
    }

    @Test
    void getsAllResourceRequirementsForSpecificSow() {
        SowResourceRequirementService service = mock(SowResourceRequirementService.class);
        SowResourceRequirementSummaryResponse summary =
                SowResourceRequirementSummaryResponse.builder().sowId(10L).build();
        when(service.getBySowId(10L)).thenReturn(summary);

        ResponseEntity<SowResourceRequirementSummaryResponse> response =
                new SowResourceRequirementController(service).getBySowId(10L);

        assertSame(summary, response.getBody());
        verify(service).getBySowId(10L);
    }

    @Test
    void getsResourceRequirementsGroupedBySow() {
        SowResourceRequirementService service = mock(SowResourceRequirementService.class);
        List<SowResourceRequirementSummaryResponse> summaries = List.of(
                SowResourceRequirementSummaryResponse.builder().sowId(10L).build());
        when(service.getAllBySow()).thenReturn(summaries);

        ResponseEntity<List<SowResourceRequirementSummaryResponse>> response =
                new SowResourceRequirementController(service).getAllBySow();

        assertSame(summaries, response.getBody());
        verify(service).getAllBySow();
    }

}
