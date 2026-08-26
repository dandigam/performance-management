package com.rit.performance.repository;

import com.rit.performance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Employee> findByIdIn(List<Long> employeeIds);

    List<Employee> findByDesignationIdInAndStatusIgnoreCase(List<Long> designationIds, String status);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByRitIdIgnoreCase(String ritId);

    boolean existsByRitIdIgnoreCaseAndIdNot(String ritId, Long id);

    boolean existsByCsxRacfIdIgnoreCase(String csxRacfId);

    boolean existsByCsxRacfIdIgnoreCaseAndIdNot(String csxRacfId, Long id);
}
