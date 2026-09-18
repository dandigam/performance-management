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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Timesheet t where t.id = :id")
    Optional<Timesheet> findForEntryUpdate(@Param("id") Long id);

    List<Timesheet> findByTimesheetEmployeeProject_IdAndWeekEndDateAfter(Long setupId, LocalDate endDate);

    boolean existsByEmployee_IdAndTimesheetEmployeeProjectIsNullAndWeekEndDateAfter(
            Long employeeId, LocalDate endDate);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM timesheets
            WHERE employee_id = :employeeId AND week_start_date = :weekStart
              AND status = 'DRAFT' AND submitted_at IS NULL
              AND regular_hours = 0 AND holiday_hours = 0 AND leave_hours = 0 AND total_hours = 0
              AND NOT EXISTS (SELECT 1 FROM timesheet_entries e WHERE e.timesheet_id = timesheets.id)
              AND NOT EXISTS (SELECT 1 FROM timesheet_approvals a WHERE a.timesheet_id = timesheets.id)
              AND NOT EXISTS (
                  SELECT 1 FROM timesheet_employee_project_day d
                  WHERE d.employee_id = timesheets.employee_id AND d.active = true
                    AND (timesheets.timesheet_employee_project_id IS NULL
                         OR d.timesheet_employee_project_id = timesheets.timesheet_employee_project_id)
                    AND d.work_date BETWEEN timesheets.week_start_date AND timesheets.week_end_date)
            """, nativeQuery = true)
    int deleteEmptyDraftWeek(@Param("employeeId") Long employeeId,
                            @Param("weekStart") LocalDate weekStart);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO timesheets (employee_id, timesheet_employee_project_id, week_start_date, week_end_date,
                regular_hours, holiday_hours, leave_hours, total_hours, status, created_at, updated_at)
            SELECT DISTINCT :employeeId, p.id, :weekStart, :weekEnd,
                0, 0, 0, 0, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM timesheet_employee_projects p
            JOIN timesheet_employee_project_day d ON d.timesheet_employee_project_id = p.id
            WHERE p.employee_id = :employeeId AND d.active = true
              AND d.work_date BETWEEN :weekStart AND :weekEnd
              AND d.work_date BETWEEN p.planned_start_date AND p.planned_end_date
              AND (p.assignment_start_date IS NULL OR d.work_date >= p.assignment_start_date)
              AND (p.assignment_end_date IS NULL OR d.work_date <= p.assignment_end_date)
              AND NOT EXISTS (SELECT 1 FROM timesheets legacy
                  WHERE legacy.employee_id = :employeeId AND legacy.week_start_date = :weekStart
                    AND legacy.timesheet_employee_project_id IS NULL)
            ON DUPLICATE KEY UPDATE id = timesheets.id
            """, nativeQuery = true)
    void insertWeeklyHeaderIfMissing(@Param("employeeId") Long employeeId,
                                    @Param("weekStart") LocalDate weekStart,
                                    @Param("weekEnd") LocalDate weekEnd);

    @Query("""
            select (count(t) > 0) from Timesheet t
            where t.employee.id = :employeeId
              and (t.timesheetEmployeeProject is null or t.timesheetEmployeeProject.id = :setupId)
              and :workDate between t.weekStartDate and t.weekEndDate
              and t.status in (com.rit.performance.entity.TimesheetStatus.SUBMITTED,
                               com.rit.performance.entity.TimesheetStatus.LEVEL1_APPROVED,
                               com.rit.performance.entity.TimesheetStatus.APPROVED)
            """)
    boolean isDateLocked(@Param("employeeId") Long employeeId,
                         @Param("workDate") LocalDate workDate, @Param("setupId") Long setupId);

    @EntityGraph(attributePaths = {"employee", "entries", "entries.sow", "entries.holiday"})
    List<Timesheet> findAllByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

    @EntityGraph(attributePaths = {"employee", "entries", "entries.sow", "entries.holiday"})
    Optional<Timesheet> findOneById(Long id);


    @EntityGraph(attributePaths = {"employee", "approvals", "approvals.approverEmployee"})
    @Query("""
            select t from Timesheet t
            where (:employeeId is null or t.employee.id = :employeeId)
              and t.weekStartDate <= :currentWeek and t.status in :statuses
            order by t.weekStartDate desc, t.id desc
            """)
    List<Timesheet> findForStatusTab(@Param("employeeId") Long employeeId,
                                   @Param("currentWeek") LocalDate currentWeek,
                                   @Param("statuses") List<com.rit.performance.entity.TimesheetStatus> statuses);
}
