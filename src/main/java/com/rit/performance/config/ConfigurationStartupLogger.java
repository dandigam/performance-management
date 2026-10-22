package com.rit.performance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ConfigurationStartupLogger {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationStartupLogger.class);
    private final Environment environment;

    public ConfigurationStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        String[] active = environment.getActiveProfiles();
        String profile = String.join(", ", active.length == 0 ? environment.getDefaultProfiles() : active);
        String configuration;
        boolean missingFile = false;
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            Path configFile = Path.of(environment.getProperty("APP_CONFIG_FILE",
                    "./config/production.properties")).toAbsolutePath().normalize();
            if (Files.isRegularFile(configFile)) {
                configuration = "Production configuration file: " + configFile;
            } else {
                configuration = "Production configuration file not found at " + configFile
                        + ". Using other configured property sources.";
                missingFile = true;
            }
        } else {
            configuration = "Application configuration: classpath:application.properties";
        }
        String banner = "\n********************************************\n"
                + "Application started. Configuration profile: " + profile + "\n"
                + configuration + "\n"
                + "********************************************";
        if (missingFile) {
            log.warn("{}", banner);
        } else {
            log.info("{}", banner);
        }
    }
}
