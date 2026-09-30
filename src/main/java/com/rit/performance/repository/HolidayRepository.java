package com.rit.performance.repository;

import com.rit.performance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    @Query("select (count(e) > 0) from TimesheetEntry e where e.holiday.id = :id")
    boolean isReferencedByEntries(@Param("id") Long id);

    @Query("select (count(d) > 0) from TimesheetEmployeeProjectDay d where d.holiday.id = :id")
    boolean isReferencedByScheduledDays(@Param("id") Long id);

    boolean existsByLocationTypeIgnoreCaseAndHolidayDate(String locationType, LocalDate holidayDate);
    boolean existsByLocationTypeIgnoreCaseAndHolidayDateAndIdNot(
            String locationType, LocalDate holidayDate, Long id);
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate startDate, LocalDate endDate);
    List<Holiday> findByLocationTypeIgnoreCaseAndHolidayDateBetweenOrderByHolidayDateAsc(
            String locationType, LocalDate startDate, LocalDate endDate);
}
