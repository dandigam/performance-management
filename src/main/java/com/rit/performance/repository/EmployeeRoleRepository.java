package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {
    boolean existsByEmployeeIdAndRoleIdAndIsCurrentTrue(Long employeeId, Long roleId);

    List<EmployeeRole> findByIsCurrentTrue();

    Optional<EmployeeRole> findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(Long employeeId);
}
