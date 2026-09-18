package com.rit.performance.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.migrations.sow-position-status.enabled",
        havingValue = "true")
public class SowPositionStatusMigrationRunner implements ApplicationRunner {

    private final SowPositionStatusMigration migration;

    @Override
    public void run(ApplicationArguments args) {
        migration.migrate();
    }
}
