package com.rit.performance.repository;

import com.rit.performance.entity.Timesheet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

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
