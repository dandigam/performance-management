package com.rit.performance.reports.timesheet;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetPdfController {
    private final TimesheetPdfService service;

    @GetMapping(value = "/{timesheetId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable Long timesheetId, Authentication authentication) {
        TimesheetPdfResult result = service.generate(timesheetId, authentication);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(result.content());
    }
}
