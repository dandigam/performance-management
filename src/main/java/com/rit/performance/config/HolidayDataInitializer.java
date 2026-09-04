package com.rit.performance.config;

import com.rit.performance.entity.Holiday;
import com.rit.performance.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Order(1)
@RequiredArgsConstructor
public class HolidayDataInitializer implements ApplicationRunner {
    private final HolidayRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // U.S. federal holidays (observed dates) for onsite employees.
        holiday("ONSITE", "New Year's Day", "2026-01-01");
        holiday("ONSITE", "Martin Luther King Jr. Day", "2026-01-19");
        holiday("ONSITE", "Presidents' Day", "2026-02-16");
        holiday("ONSITE", "Memorial Day", "2026-05-25");
        holiday("ONSITE", "Juneteenth National Independence Day", "2026-06-19");
        holiday("ONSITE", "Independence Day (Observed)", "2026-07-03");
        holiday("ONSITE", "Labor Day", "2026-09-07");
        holiday("ONSITE", "Columbus Day", "2026-10-12");
        holiday("ONSITE", "Veterans Day", "2026-11-11");
        holiday("ONSITE", "Thanksgiving Day", "2026-11-26");
        holiday("ONSITE", "Christmas Day", "2026-12-25");

        // Common India public holidays for offshore employees.
        holiday("OFFSHORE", "Republic Day", "2026-01-26");
        holiday("OFFSHORE", "Holi", "2026-03-04");
        holiday("OFFSHORE", "Good Friday", "2026-04-03");
        holiday("OFFSHORE", "Independence Day", "2026-08-15");
        holiday("OFFSHORE", "Gandhi Jayanti", "2026-10-02");
        holiday("OFFSHORE", "Dussehra", "2026-10-20");
        holiday("OFFSHORE", "Diwali", "2026-11-08");
        holiday("OFFSHORE", "Christmas Day", "2026-12-25");
    }

    private void holiday(String location, String name, String date) {
        LocalDate holidayDate = LocalDate.parse(date);
        if (!repository.existsByLocationTypeIgnoreCaseAndHolidayDate(location, holidayDate)) {
            repository.save(Holiday.builder()
                    .holidayName(name)
                    .holidayDate(holidayDate)
                    .locationType(location)
                    .description(location.equals("ONSITE")
                            ? "2026 U.S. holiday calendar" : "2026 India holiday calendar")
                    .active(true)
                    .build());
        }
    }
}
