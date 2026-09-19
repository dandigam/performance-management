package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeLeavePolicy;
import com.rit.performance.entity.LeavePolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface EmployeeLeavePolicyRepository extends JpaRepository<EmployeeLeavePolicy, Long> {
    List<EmployeeLeavePolicy> findByEmployeeIdOrderByEffectiveFromDescIdDesc(Long employeeId);

    @Query("""
            select a from EmployeeLeavePolicy a
            where a.employee.id = :employeeId and a.status = :status
              and a.effectiveFrom <= :fromDate
              and (a.effectiveTo is null or a.effectiveTo >= :toDate)
            """)
    List<EmployeeLeavePolicy> findCovering(@Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
            @Param("status") LeavePolicyStatus status);

    @Query("""
            select count(a) from EmployeeLeavePolicy a
            where a.employee.id = :employeeId and a.status = :status and a.id <> :excludedId
              and a.effectiveFrom <= :endDate
              and (a.effectiveTo is null or a.effectiveTo >= :startDate)
            """)
    long countOverlaps(@Param("employeeId") Long employeeId, @Param("status") LeavePolicyStatus status,
                       @Param("excludedId") Long excludedId, @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);

    @Query("""
            select a from EmployeeLeavePolicy a
            where a.employee.id = :employeeId and a.status = :status
              and a.effectiveFrom <= :date and (a.effectiveTo is null or a.effectiveTo >= :date)
            """)
    List<EmployeeLeavePolicy> findCurrent(@Param("employeeId") Long employeeId,
                                           @Param("status") LeavePolicyStatus status,
                                           @Param("date") LocalDate date);
}
