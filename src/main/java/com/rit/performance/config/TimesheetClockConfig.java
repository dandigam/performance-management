package com.rit.performance.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimesheetClockConfig {

    @Bean
    public Clock timesheetClock() {
        return Clock.systemDefaultZone();
    }
}
