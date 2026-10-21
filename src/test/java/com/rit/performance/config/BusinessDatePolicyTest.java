package com.rit.performance.config;

import com.rit.performance.controller.DateContextController;
import jakarta.validation.constraints.PastOrPresent;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BusinessDatePolicyTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), ZoneId.of("America/New_York"));

    record DatedRequest(@PastOrPresent LocalDate date) {}

    @Test void indiaNextDayUsesEasternBusinessDateAndUtcAuditTime() {
        assertThat(LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata"))))
                .isEqualTo(LocalDate.of(2026, 10, 10));
        var response = new DateContextController(clock).get();
        assertThat(response.getBody().businessTimeZone()).isEqualTo("America/New_York");
        assertThat(response.getBody().currentBusinessDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(response.getBody().currentInstant()).isEqualTo(clock.instant());
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(new JpaAuditingConfiguration().utcAuditTime(clock).getNow()).contains(LocalDateTime.of(2026, 10, 10, 2, 0));
    }

    @Test void dateValidationUsesBusinessClockWithoutChangingSubmittedDate() {
        try (var validator = new BusinessDateValidationConfiguration().businessDateValidation(clock)) {
            validator.afterPropertiesSet();
            var request = new DatedRequest(LocalDate.of(2026, 10, 10));
            assertThat(validator.validate(request)).hasSize(1);
            assertThat(request.date()).isEqualTo(LocalDate.of(2026, 10, 10));
            assertThat(validator.validate(new DatedRequest(LocalDate.of(2026, 10, 9)))).isEmpty();
        }
    }

    @Test void configuredZoneUsesDaylightSavingRules() {
        Clock eastern = new TimesheetClockConfig().timesheetClock("America/New_York");
        assertThat(eastern.getZone().getRules().getOffset(Instant.parse("2026-01-01T12:00:00Z"))).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(eastern.getZone().getRules().getOffset(clock.instant())).isEqualTo(ZoneOffset.ofHours(-4));
    }
}
