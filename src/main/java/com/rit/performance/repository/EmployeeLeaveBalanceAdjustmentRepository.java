package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeLeaveBalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeLeaveBalanceAdjustmentRepository extends JpaRepository<EmployeeLeaveBalanceAdjustment, Long> {
    List<EmployeeLeaveBalanceAdjustment> findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(Long balanceId);
}
