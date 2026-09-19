package com.rit.performance.repository;

import com.rit.performance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForLeavePolicyUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("""
            select e from Employee e
            where (:status is null or upper(e.status) = :status)
              and (:workMode is null or upper(e.workMode) = :workMode)
              and (:assignmentStatus is null
                or (:assignmentStatus = 'ASSIGNED' and exists (
                    select a.id from EmployeeAssignment a where a.employeeId = e.id
                    and upper(a.status) = 'ASSIGNED' and a.effectiveFrom <= :today
                    and (a.effectiveTo is null or a.effectiveTo >= :today)))
                or (:assignmentStatus = 'UNASSIGNED' and not exists (
                    select a.id from EmployeeAssignment a where a.employeeId = e.id
                    and upper(a.status) = 'ASSIGNED' and a.effectiveFrom <= :today
                    and (a.effectiveTo is null or a.effectiveTo >= :today))))
              and ((:departmentId is null and :sowId is null) or exists (
                    select a.id from EmployeeAssignment a, Sow s
                    where a.employeeId = e.id and a.sowId = s.id
                    and upper(a.status) = 'ASSIGNED' and a.effectiveFrom <= :today
                    and (a.effectiveTo is null or a.effectiveTo >= :today)
                    and (:departmentId is null or s.businessUnit.id = :departmentId)
                    and (:sowId is null or s.id = :sowId)))
              and (:search is null
                or lower(concat(concat(coalesce(e.firstName, ''), ' '), coalesce(e.lastName, ''))) like :search escape '!'
                or lower(e.ritId) like :search escape '!'
                or lower(e.email) like :search escape '!'
                or exists (select d.id from LookupValue d where d.id = e.designationId
                    and lower(d.name) like :search escape '!')
                or exists (select a.id from EmployeeAssignment a, Sow s
                    left join s.businessUnit department
                    where a.employeeId = e.id and a.sowId = s.id
                    and upper(a.status) = 'ASSIGNED' and a.effectiveFrom <= :today
                    and (a.effectiveTo is null or a.effectiveTo >= :today)
                    and (lower(s.sowName) like :search escape '!'
                        or lower(department.name) like :search escape '!')))
            """)
    org.springframework.data.domain.Page<Employee> findSummaries(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("sowId") Long sowId,
            @org.springframework.data.repository.query.Param("assignmentStatus") String assignmentStatus,
            @org.springframework.data.repository.query.Param("workMode") String workMode,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("today") java.time.LocalDate today,
            org.springframework.data.domain.Pageable pageable);

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
