package com.rit.performance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Removes the legacy SOW type column after SOW Type was removed from rate cards. */
@Component
@RequiredArgsConstructor
public class RateCardSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer columnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'rate_cards'
                  and column_name = 'sow_type_id'
                """, Integer.class);
        if (columnCount == null || columnCount == 0) return;

        List<String> foreignKeys = jdbcTemplate.queryForList("""
                select constraint_name
                from information_schema.key_column_usage
                where table_schema = database()
                  and table_name = 'rate_cards'
                  and column_name = 'sow_type_id'
                  and referenced_table_name is not null
                """, String.class);
        for (String foreignKey : foreignKeys) {
            jdbcTemplate.execute("alter table rate_cards drop foreign key `"
                    + foreignKey.replace("`", "``") + "`");
        }
        jdbcTemplate.execute("alter table rate_cards drop column sow_type_id");
    }
}
