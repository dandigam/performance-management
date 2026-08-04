package com.rit.performance.repository;

import com.rit.performance.entity.CsxEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsxEmployeeRepository extends JpaRepository<CsxEmployee, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
