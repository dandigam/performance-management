package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {
    boolean existsByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(Long assignmentId, Long leaveTypeId, int year);
    List<EmployeeLeaveBalance> findByEmployeeLeavePolicyIdAndBalanceYearOrderByIdAsc(Long assignmentId, int year);
    List<EmployeeLeaveBalance> findByEmployeeIdAndBalanceYearOrderByIdAsc(Long employeeId, int year);
    Optional<EmployeeLeaveBalance> findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(
            Long assignmentId, Long leaveTypeId, int year);
}
