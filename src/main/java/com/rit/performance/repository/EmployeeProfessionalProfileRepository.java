package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeProfessionalProfileRepository extends JpaRepository<EmployeeProfessionalProfile, Long> {
    Optional<EmployeeProfessionalProfile> findByEmployeeId(Long employeeId);
}
