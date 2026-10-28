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
    @Query("""
            select new com.rit.performance.dto.ReportingTeamMemberResponse(
                e.id, trim(concat(coalesce(e.firstName, ''), ' ', coalesce(e.lastName, ''))),
                e.ritId, d.name, e.status)
            from EmployeeOrgHierarchy h
            join h.employee e
            left join LookupValue d on d.id = e.designationId
            where h.reportingManager.id = :managerEmployeeId and upper(e.status) = 'ACTIVE'
            order by lower(trim(concat(coalesce(e.firstName, ''), ' ', coalesce(e.lastName, '')))), e.id
            """)
    List<com.rit.performance.dto.ReportingTeamMemberResponse> findReportingTeam(
            @org.springframework.data.repository.query.Param("managerEmployeeId") Long managerEmployeeId);

    Optional<EmployeeOrgHierarchy> findByEmployee_Id(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "reportingManager"})
    List<EmployeeOrgHierarchy> findAllByOrderByIdAsc();

    // Every hierarchy writer locks the same first employee, including when this table is empty.
    // This prevents concurrent updates from jointly introducing a reporting cycle.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e order by e.id")
    List<Employee> lockHierarchyWrites(Pageable pageable);
}
