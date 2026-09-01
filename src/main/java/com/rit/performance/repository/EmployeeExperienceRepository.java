package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeExperienceRepository extends JpaRepository<EmployeeExperience, Long> {
    List<EmployeeExperience> findByEmployeeIdOrderByFromDateDescIdDesc(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
