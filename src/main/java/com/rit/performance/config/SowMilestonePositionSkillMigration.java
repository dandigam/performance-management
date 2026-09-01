package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Backfills position skills from their selected rate cards where possible. */
@Component
@Order(200)
@RequiredArgsConstructor
public class SowMilestonePositionSkillMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("""
                update sow_milestone_positions position_row
                join rate_cards rate_card on rate_card.id = position_row.rate_card_id
                set position_row.skill_id = rate_card.main_skill_id
                where position_row.skill_id is null
                  and rate_card.main_skill_id is not null
                """);
    }
}
