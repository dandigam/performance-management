package com.rit.performance.repository;

import com.rit.performance.entity.TimesheetEmployeeProjectDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimesheetEmployeeProjectDayRepository
        extends JpaRepository<TimesheetEmployeeProjectDay, Long> {
    List<TimesheetEmployeeProjectDay> findByTimesheetEmployeeProjectIdOrderByWorkDate(Long projectId);

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
