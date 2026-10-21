package com.rit.performance.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimesheetClockConfig {

    @Bean
    public Clock timesheetClock(@org.springframework.beans.factory.annotation.Value("${app.business.time-zone:America/New_York}") String zone) {
        return Clock.system(java.time.ZoneId.of(zone));
    }
}
