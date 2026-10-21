package com.rit.performance.service;

import com.rit.performance.dto.report.ReportExportRequest;
import com.rit.performance.dto.report.ReportFilterRequest;
import com.rit.performance.dto.report.ReportQueryRequest;
import com.rit.performance.dto.report.ReportSortRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.SowRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SowMilestoneReportServiceTest {

    @Test
    void definitionIncludesStatusesUsedByExistingMilestones() {
        SowMilestoneReportService service = new SowMilestoneReportService(mock(SowRepository.class));

        var milestoneStatus = service.definition().columns().stream()
                .filter(column -> "milestoneStatus".equals(column.key())).findFirst().orElseThrow();

        assertThat(milestoneStatus.options()).extracting("value")
                .contains("PLANNING", "NOT_STARTED", "IN_PROGRESS", "COMPLETED");
    }

    @Test
    void returnsMilestoneRowsAndBlankRowForSowWithoutMilestones() {
        SowRepository repository = mock(SowRepository.class);
        when(repository.findAllWithDetails()).thenReturn(List.of(sowWithMilestone(), sowWithoutMilestones()));
        SowMilestoneReportService service = new SowMilestoneReportService(repository);

        var response = service.query(new ReportQueryRequest(
                List.of("sowNumber", "sowName", "milestoneName", "positionCount", "currency"),
                List.of(), List.of(new ReportSortRequest("sowName", "ASC")), 0, 25));

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0)).containsEntry("sowName", "Alpha SOW")
                .containsEntry("milestoneName", "Discovery")
                .containsEntry("positionCount", 1)
                .containsEntry("currency", "USD");
        assertThat(response.content().get(1)).containsEntry("sowName", "Beta SOW")
                .containsEntry("milestoneName", null);
        assertThat(response.summary()).containsEntry("totalSows", 2L)
                .containsEntry("activeSows", 1L)
                .containsEntry("totalMilestones", 1L)
                .containsEntry("totalPlannedHours", 120L)
                .containsEntry("totalPositions", 1L);
    }

    @Test
    void appliesCombinedFiltersAndSummaryToFilteredRows() {
        SowRepository repository = mock(SowRepository.class);
        when(repository.findAllWithDetails()).thenReturn(List.of(sowWithMilestone(), sowWithoutMilestones()));
        SowMilestoneReportService service = new SowMilestoneReportService(repository);

        var response = service.query(new ReportQueryRequest(
                List.of("sowName", "milestoneName"),
                List.of(
                        new ReportFilterRequest("sowStatus", "EQUALS", StringNode.valueOf("ACTIVE")),
                        new ReportFilterRequest("milestoneStatus", "EQUALS", StringNode.valueOf("IN_PROGRESS"))),
                List.of(), 0, 25));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0))
                .containsEntry("sowName", "Alpha SOW")
                .containsEntry("milestoneName", "Discovery");
        assertThat(response.summary()).containsEntry("totalSows", 1L)
                .containsEntry("activeSows", 1L)
                .containsEntry("totalMilestones", 1L);
    }

    @Test
    void exportUsesSelectedColumnOrderAndStatusLabels() {
        SowRepository repository = mock(SowRepository.class);
        when(repository.findAllWithDetails()).thenReturn(List.of(sowWithMilestone()));
        SowMilestoneReportService service = new SowMilestoneReportService(repository);

        var export = service.exportData(new ReportExportRequest(
                List.of("milestoneName", "sowStatus", "sowName"), List.of(), List.of()));

        assertThat(export.columns()).extracting("key")
                .containsExactly("milestoneName", "sowStatus", "sowName");
        assertThat(export.content()).singleElement().satisfies(row -> {
            assertThat(row.keySet()).containsExactly("milestoneName", "sowStatus", "sowName");
            assertThat(row).containsEntry("sowStatus", "Active");
        });
    }

    private Sow sowWithMilestone() {
        LookupType type = LookupType.builder().id(1L).code("SOW_STATUS").name("SOW Status").build();
        LookupValue active = LookupValue.builder().id(2L).lookupType(type).code("ACTIVE").name("Active").build();
        LookupValue business = LookupValue.builder().id(3L).lookupType(type).code("ENG").name("Engineering").build();
        Client client = Client.builder().id(4L).clientName("CSX").build();
        Sow sow = Sow.builder().id(10L).sowName("Alpha SOW").csxProjectId("SOW-001")
                .sowType("T&M").engagementType("PROJECT").client(client).businessUnit(business)
                .status(active).startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31)).build();
        RateCard rateCard = RateCard.builder().id(20L).client(client).hourlyRate(BigDecimal.TEN)
                .currency("USD").effectiveFrom(LocalDate.of(2026, 1, 1)).status("ACTIVE").build();
        SowMilestone milestone = SowMilestone.builder().id(30L).sow(sow).milestoneName("Discovery")
                .displayOrder(1).estimatedHours(120).startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 2, 1)).amount(new BigDecimal("25000"))
                .status("IN_PROGRESS").build();
        SowMilestonePosition position = SowMilestonePosition.builder().id(40L).sow(sow).milestone(milestone)
                .rateCard(rateCard).positionName("Technical Lead").positionType("BILLABLE")
                .status("OPEN").build();
        milestone.setPositions(new LinkedHashSet<>(List.of(position)));
        sow.setMilestones(new LinkedHashSet<>(List.of(milestone)));
        return sow;
    }

    private Sow sowWithoutMilestones() {
        LookupType type = LookupType.builder().id(5L).code("SOW_STATUS").name("SOW Status").build();
        LookupValue draft = LookupValue.builder().id(6L).lookupType(type).code("DRAFT").name("Draft").build();
        return Sow.builder().id(11L).sowName("Beta SOW").csxProjectId("SOW-002")
                .sowType("FIXED").engagementType("PROJECT").status(draft)
                .milestones(new LinkedHashSet<>()).build();
    }
}
