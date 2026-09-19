package com.rit.performance.repository;

import com.rit.performance.entity.TimesheetEmployeeProjectDay;
import com.rit.performance.entity.TimesheetScheduleStatus;
import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimesheetEmployeeProjectDayRepository
        extends JpaRepository<TimesheetEmployeeProjectDay, Long> {
    @Query("""
            select day from TimesheetEmployeeProjectDay day
            join day.timesheetEmployeeProject project
            where day.employee.id = :employeeId
              and day.workDate between :fromDate and :toDate
              and day.active = true and day.status = :dayStatus and day.scheduledHours > 0
              and project.status = :projectStatus
            order by day.workDate
            """)
    List<TimesheetEmployeeProjectDay> findLeaveSchedule(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("dayStatus") TimesheetScheduleStatus dayStatus,
            @Param("projectStatus") TimesheetEmployeeProjectStatus projectStatus);
    List<TimesheetEmployeeProjectDay> findByTimesheetEmployeeProjectIdOrderByWorkDate(Long projectId);

    List<TimesheetEmployeeProjectDay> findByTimesheetEmployeeProjectIdAndWorkDateIn(
            Long projectId, java.util.Collection<LocalDate> workDates);

    @Query("""
            select coalesce(sum(day.scheduledHours), 0)
            from TimesheetEmployeeProjectDay day
            where day.employee.id = :employeeId
              and day.workDate = :workDate
              and day.active = true
              and day.timesheetEmployeeProject.id <> :excludedProjectId
            """)
    BigDecimal sumOtherProjectHours(@Param("employeeId") Long employeeId,
                                    @Param("workDate") LocalDate workDate,
                                    @Param("excludedProjectId") Long excludedProjectId);
}
