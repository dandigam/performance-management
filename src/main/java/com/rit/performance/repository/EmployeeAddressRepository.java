package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeAddressRepository extends JpaRepository<EmployeeAddress, Long> {
    Optional<EmployeeAddress> findByEmployeeId(Long employeeId);
    List<EmployeeAddress> findByEmployeeIdIn(List<Long> employeeIds);
}
