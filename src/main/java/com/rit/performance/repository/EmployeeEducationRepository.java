package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeEducationRepository extends JpaRepository<EmployeeEducation, Long> {
    List<EmployeeEducation> findByEmployeeIdOrderByPassingYearDescIdDesc(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}
