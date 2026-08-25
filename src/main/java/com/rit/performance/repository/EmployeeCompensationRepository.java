package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EmployeeCompensationRepository extends JpaRepository<EmployeeCompensation, Long> {
    Optional<EmployeeCompensation> findFirstByEmployeeIdAndCurrentTrueOrderByEffectiveDateDescIdDesc(Long employeeId);
    List<EmployeeCompensation> findByEmployeeIdOrderByEffectiveDateDescIdDesc(Long employeeId);
}
