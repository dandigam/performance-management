package com.rit.performance.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DateContextController {
    private final Clock clock;

    public DateContextController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/api/v1/date-context")
    public ResponseEntity<DateContext> get() {
        Instant now = clock.instant();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new DateContext(clock.getZone().getId(),
                        now.atZone(clock.getZone()).toLocalDate(), now));
    }

    public record DateContext(String businessTimeZone, LocalDate currentBusinessDate, Instant currentInstant) {}
}
