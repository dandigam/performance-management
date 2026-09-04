package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeAuditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeAuditHistoryRepository extends JpaRepository<EmployeeAuditHistory, Long> {
    List<EmployeeAuditHistory> findByEmployeeIdOrderByChangedOnDescIdDesc(Long employeeId);
}
