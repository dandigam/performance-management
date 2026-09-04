package com.rit.performance.repository;

import com.rit.performance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByLocationTypeIgnoreCaseAndHolidayDate(String locationType, LocalDate holidayDate);
    boolean existsByLocationTypeIgnoreCaseAndHolidayDateAndIdNot(
            String locationType, LocalDate holidayDate, Long id);
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate startDate, LocalDate endDate);
    List<Holiday> findByLocationTypeIgnoreCaseAndHolidayDateBetweenOrderByHolidayDateAsc(
            String locationType, LocalDate startDate, LocalDate endDate);
}
