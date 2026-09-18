package com.rit.performance.repository;

import com.rit.performance.entity.TimesheetEmployeeProject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimesheetEmployeeProjectRepository
        extends JpaRepository<TimesheetEmployeeProject, Long> {

    Optional<TimesheetEmployeeProject> findByMilestonePositionAssignment_Id(Long assignmentId);

    List<TimesheetEmployeeProject> findAllByEmployeeIdAndSowIdAndMilestoneId(Long employeeId, Long sowId, Long milestoneId);

    @EntityGraph(attributePaths = {
            "employee", "sow", "sow.client", "milestone", "level1Approver", "level2Approver"
    })
    Optional<TimesheetEmployeeProject> findByEmployeeIdAndSowIdAndMilestoneId(
            Long employeeId, Long sowId, Long milestoneId);

    @EntityGraph(attributePaths = {
            "employee", "sow", "sow.client", "milestone", "level1Approver", "level2Approver",
            "dailySchedules", "dailySchedules.holiday"
    })
    List<TimesheetEmployeeProject> findByEmployeeIdOrderByStartDateDescIdDesc(Long employeeId);

    @EntityGraph(attributePaths = {
            "employee", "sow", "milestone", "level1Approver", "level2Approver"
    })
    List<TimesheetEmployeeProject> findAllByEmployeeIdOrderByStartDateAscIdAsc(Long employeeId);

}
