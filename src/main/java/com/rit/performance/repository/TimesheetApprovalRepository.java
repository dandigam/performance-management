package com.rit.performance.repository;

import com.rit.performance.entity.TimesheetApproval;
import com.rit.performance.entity.TimesheetApprovalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimesheetApprovalRepository extends JpaRepository<TimesheetApproval, Long> {
    @EntityGraph(attributePaths = {"timesheet", "timesheet.employee", "timesheet.timesheetEmployeeProject"})
    @Query("""
            select a from TimesheetApproval a
            where a.approverEmployee.id = :reviewerEmployeeId
              and (:statuses is null or a.status in :statuses)
            order by a.timesheet.weekStartDate desc, a.timesheet.id desc, a.approvalLevel asc
            """)
    List<TimesheetApproval> findForReviewer(
            @Param("reviewerEmployeeId") Long reviewerEmployeeId,
            @Param("statuses") List<TimesheetApprovalStatus> statuses);
}