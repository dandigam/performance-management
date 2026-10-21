package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {
    boolean existsByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(Long assignmentId, Long leaveTypeId, int year);
    List<EmployeeLeaveBalance> findByEmployeeLeavePolicyIdAndBalanceYearOrderByIdAsc(Long assignmentId, int year);
    List<EmployeeLeaveBalance> findByEmployeeIdAndBalanceYearOrderByIdAsc(Long employeeId, int year);
    Optional<EmployeeLeaveBalance> findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(
            Long assignmentId, Long leaveTypeId, int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from EmployeeLeaveBalance b
            where b.employeeLeavePolicy.id = :assignmentId
              and b.leaveType.id = :leaveTypeId and b.balanceYear = :year
            """)
    Optional<EmployeeLeaveBalance> findForApproval(@Param("assignmentId") Long assignmentId,
            @Param("leaveTypeId") Long leaveTypeId, @Param("year") int year);
}
