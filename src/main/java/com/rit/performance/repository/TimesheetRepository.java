package com.rit.performance.repository;

import com.rit.performance.entity.Timesheet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    @Query("""
            select (count(t) > 0) from Timesheet t
            where t.employee.id = :employeeId
              and :workDate between t.weekStartDate and t.weekEndDate
              and t.status in (com.rit.performance.entity.TimesheetStatus.SUBMITTED,
                               com.rit.performance.entity.TimesheetStatus.LEVEL1_APPROVED,
                               com.rit.performance.entity.TimesheetStatus.APPROVED)
            """)
    boolean isDateLocked(@Param("employeeId") Long employeeId,
                         @Param("workDate") LocalDate workDate);

    @EntityGraph(attributePaths = {"employee", "entries", "entries.sow", "entries.holiday"})
    Optional<Timesheet> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

    @EntityGraph(attributePaths = {"employee", "entries", "entries.sow", "entries.holiday"})
    Optional<Timesheet> findOneById(Long id);

    List<Timesheet> findByEmployeeIdAndWeekStartDateBetween(
            Long employeeId, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"employee", "approvals"})
    List<Timesheet> findAllByOrderByWeekStartDateDescIdDesc();

    @EntityGraph(attributePaths = {"employee", "approvals"})
    List<Timesheet> findByEmployeeIdOrderByWeekStartDateDescIdDesc(Long employeeId);
}
