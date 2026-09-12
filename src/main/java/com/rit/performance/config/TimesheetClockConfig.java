package com.rit.performance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class TimesheetClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
