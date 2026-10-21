package com.rit.performance.repository;

import com.rit.performance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByClient_IdAndLocationTypeIgnoreCaseAndHolidayDate(
            Long clientId, String locationType, LocalDate holidayDate);
    boolean existsByClient_IdAndLocationTypeIgnoreCaseAndHolidayDateAndIdNot(
            Long clientId, String locationType, LocalDate holidayDate, Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "client")
    @Query("""
            select h from Holiday h
            where (:clientId is null or h.client.id = :clientId)
              and h.holidayDate between :start and :end
              and (:location is null or upper(h.locationType) = :location)
              and (:active is null or h.active = :active)
            order by h.holidayDate, h.id
            """)
    List<Holiday> findForCalendar(@Param("clientId") Long clientId,
            @Param("start") LocalDate start, @Param("end") LocalDate end,
            @Param("location") String location, @Param("active") Boolean active);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holiday h where h.id = :id")
    java.util.Optional<Holiday> findForUpdate(@Param("id") Long id);

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
