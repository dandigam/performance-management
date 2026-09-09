package com.rit.performance.repository;

import com.rit.performance.entity.TimesheetEmployeeProject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetEmployeeProjectRepository
        extends JpaRepository<TimesheetEmployeeProject, Long> {

    Optional<TimesheetEmployeeProject> findByEmployeeIdAndSowIdAndStartDate(
            Long employeeId, Long sowId, LocalDate startDate);

    @EntityGraph(attributePaths = {
            "employee", "sow", "sow.client", "level1Approver", "level2Approver"
    })
    List<TimesheetEmployeeProject> findByEmployeeIdOrderByStartDateDescIdDesc(Long employeeId);

    @Query("""
            select distinct tep.employee.id
            from TimesheetEmployeeProject tep
            where tep.status = com.rit.performance.entity.TimesheetEmployeeProjectStatus.ACTIVE
              and tep.startDate <= :weekEndDate
              and (tep.endDate is null or tep.endDate >= :weekStartDate)
              and not exists (
                  select timesheet.id
                  from Timesheet timesheet
                  where timesheet.employee.id = tep.employee.id
                    and timesheet.weekStartDate = :weekStartDate
              )
            order by tep.employee.id
            """)
    List<Long> findEmployeeIdsEligibleForTimesheetGeneration(
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate);
}
