package com.rit.performance.repository;

import com.rit.performance.entity.LeaveRequest;
import com.rit.performance.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedOnDescIdDesc(Long employeeId);
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);
    List<LeaveRequest> findAllByOrderBySubmittedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LeaveRequest r where r.id = :id")
    Optional<LeaveRequest> findByIdForApproval(@Param("id") Long id);

    @Query("""
            select distinct r from LeaveRequest r join r.days d
            where r.employee.id = :employeeId and r.id <> :excludedId
              and r.status in :statuses and d.leaveDate in :dates
            """)
    List<LeaveRequest> findBlockingRequests(@Param("employeeId") Long employeeId,
            @Param("excludedId") Long excludedId,
            @Param("statuses") Collection<LeaveRequestStatus> statuses,
            @Param("dates") Collection<LocalDate> dates);
}
