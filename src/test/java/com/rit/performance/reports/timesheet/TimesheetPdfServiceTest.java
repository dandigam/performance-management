package com.rit.performance.reports.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetPdfServiceTest {
    @Test
    void generatesPdfAndSafeFilename() {
        TimesheetPdfDataService data = mock(TimesheetPdfDataService.class);
        LocalDate start = LocalDate.of(2026, 8, 30);
        List<TimesheetPdfDayHeader> headers = start.datesUntil(start.plusDays(7))
                .map(d -> new TimesheetPdfDayHeader(d, d.getDayOfWeek().name())).toList();
        List<TimesheetPdfDay> days = headers.stream().map(d -> new TimesheetPdfDay(d.date(), d.label(),
                BigDecimal.valueOf(8), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(8))).toList();
        TimesheetPdfDocument document = new TimesheetPdfDocument(125L, "RIT03", "Charan Kovvuru",
                "ONSITE", start, start.plusDays(6), "SUBMITTED", "Submitted", null,
                BigDecimal.valueOf(40), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), OffsetDateTime.now(), null, headers,
                List.of(new TimesheetPdfProject(1L, "Project", "SOW", "Milestone", "Developer",
                        "Client", days, BigDecimal.valueOf(40), BigDecimal.ZERO, BigDecimal.valueOf(40))),
                List.of(), OffsetDateTime.now());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
        when(data.loadAuthorized(125L, authentication)).thenReturn(document);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        TimesheetPdfResult result = new TimesheetPdfService(data, engine).generate(125L, authentication);

        assertEquals("timesheet-RIT03-2026-08-30.pdf", result.fileName());
        assertTrue(result.content().length > 100);
        assertEquals("%PDF", new String(result.content(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }
}
