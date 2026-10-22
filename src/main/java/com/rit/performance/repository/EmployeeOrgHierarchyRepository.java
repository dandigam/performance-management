package com.rit.performance.repository;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeOrgHierarchy;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeOrgHierarchyRepository extends JpaRepository<EmployeeOrgHierarchy, Long> {
    Optional<EmployeeOrgHierarchy> findByEmployee_Id(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "reportingManager"})
    List<EmployeeOrgHierarchy> findAllByOrderByIdAsc();

    // Every hierarchy writer locks the same first employee, including when this table is empty.
    // This prevents concurrent updates from jointly introducing a reporting cycle.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e order by e.id")
    List<Employee> lockHierarchyWrites(Pageable pageable);
}
