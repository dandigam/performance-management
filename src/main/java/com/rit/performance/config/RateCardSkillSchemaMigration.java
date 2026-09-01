package com.rit.performance.config;

import com.rit.performance.entity.LookupType;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.LookupTypeRepository;
import com.rit.performance.repository.LookupValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Migrates the former free-text rate card skill into SKILL lookup values. */
@Component
@Order(100)
@RequiredArgsConstructor
public class RateCardSkillSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final LookupTypeRepository lookupTypeRepository;
    private final LookupValueRepository lookupValueRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LookupType skillType = lookupTypeRepository.findByCodeIgnoreCase("SKILL")
                .orElseGet(() -> lookupTypeRepository.save(LookupType.builder()
                        .code("SKILL")
                        .name("Skills")
                        .build()));
        remapMainSkillValues(skillType);

        Integer legacyColumnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'rate_cards'
                  and column_name = 'skill'
                """, Integer.class);
        if (legacyColumnCount == null || legacyColumnCount == 0) return;
        List<String> legacySkills = jdbcTemplate.queryForList("""
                select distinct trim(skill)
                from rate_cards
                where skill is not null and trim(skill) <> ''
                """, String.class);
        int displayOrder = lookupValueRepository
                .findByLookupTypeIdOrderByDisplayOrderAscIdAsc(skillType.getId()).size() + 1;
        for (String skill : legacySkills) {
            LookupValue value = skillValue(skillType, skill, displayOrder);
            jdbcTemplate.update("""
                    update rate_cards
                    set main_skill_id = ?
                    where trim(skill) = ? and main_skill_id is null
                    """, value.getId(), skill);
        }
        jdbcTemplate.execute("alter table rate_cards drop column skill");
    }

    private void remapMainSkillValues(LookupType skillType) {
        List<java.util.Map<String, Object>> values = jdbcTemplate.queryForList("""
                select distinct value.id, value.name
                from rate_cards card
                join lookup_values value on value.id = card.main_skill_id
                join lookup_types type on type.id = value.lookup_type_id
                where upper(type.code) = 'MAIN_SKILL'
                """);
        int displayOrder = lookupValueRepository
                .findByLookupTypeIdOrderByDisplayOrderAscIdAsc(skillType.getId()).size() + 1;
        for (java.util.Map<String, Object> row : values) {
            Long oldId = ((Number) row.get("id")).longValue();
            String name = row.get("name").toString();
            LookupValue replacement = skillValue(skillType, name, displayOrder);
            jdbcTemplate.update(
                    "update rate_cards set main_skill_id = ? where main_skill_id = ?",
                    replacement.getId(), oldId);
        }
    }

    private LookupValue skillValue(LookupType skillType, String skill, int displayOrder) {
        String code = codeFor(skill);
        return lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "SKILL", code)
                .orElseGet(() -> lookupValueRepository.save(LookupValue.builder()
                        .lookupType(skillType)
                        .code(code)
                        .name(skill)
                        .displayOrder(displayOrderFor(skill, displayOrder))
                        .build()));
    }

    private int displayOrderFor(String skill, int startingOrder) {
        return startingOrder + Math.abs(skill.hashCode() % 10000);
    }

    private String codeFor(String skill) {
        String normalized = skill.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) normalized = "SKILL";
        if (normalized.length() <= 40) return normalized;
        return normalized.substring(0, 40) + "_" + Integer.toHexString(skill.hashCode())
                .toUpperCase(Locale.ROOT);
    }
}
