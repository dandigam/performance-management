package com.rit.performance.reports.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimesheetPdfControllerTest {
    @Test
    void returnsPdfAsNoStoreAttachment() {
        TimesheetPdfService service = mock(TimesheetPdfService.class);
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
        when(service.generate(125L, authentication))
                .thenReturn(new TimesheetPdfResult("timesheet-RIT03-2026-08-30.pdf", pdf));

        ResponseEntity<byte[]> response = new TimesheetPdfController(service).download(125L, authentication);

        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertEquals("no-store, no-cache, must-revalidate", response.getHeaders().getCacheControl());
        assertArrayEquals(pdf, response.getBody());
    }
}
