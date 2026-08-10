package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, Long> {

    List<EmployeeAssignment> findByEmployeeId(Long employeeId);

    Optional<EmployeeAssignment> findFirstByEmployeeIdOrderByEffectiveFromDescIdDesc(Long employeeId);

    Optional<EmployeeAssignment> findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDescIdDesc(Long employeeId);

    default Optional<EmployeeAssignment> findByEmployeeIdAndIsCurrentTrue(Long employeeId) {
        return findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDescIdDesc(employeeId);
    }

    @Query("""
            select assignment from EmployeeAssignment assignment
            where assignment.employeeId = :employeeId
              and assignment.effectiveFrom <= :onDate
              and (assignment.effectiveTo is null or assignment.effectiveTo >= :onDate)
            order by assignment.effectiveFrom desc
            """)
    List<EmployeeAssignment> findEffectiveOnDate(@Param("employeeId") Long employeeId,
            @Param("onDate") java.time.LocalDate onDate);

    List<EmployeeAssignment> findByIsCurrentTrue();

    List<EmployeeAssignment> findByManagerId(Long managerId);

    List<EmployeeAssignment> findByManagerIdAndIsCurrentTrue(Long managerId);

    List<EmployeeAssignment> findByLeadId(Long leadId);

    List<EmployeeAssignment> findByLeadIdAndIsCurrentTrue(Long leadId);

    List<EmployeeAssignment> findByDepartmentId(Long departmentId);

    List<EmployeeAssignment> findByDepartmentIdInAndIsCurrentTrue(List<Long> departmentIds);

    List<EmployeeAssignment> findByDesignationIdInAndIsCurrentTrue(List<Long> designationIds);

    List<EmployeeAssignment> findByProjectId(Long projectId);

    Page<EmployeeAssignment> findByProjectIdAndIsCurrentTrue(Long projectId, Pageable pageable);

    boolean existsByProjectIdAndEmployeeIdAndIsCurrentTrue(Long projectId, Long employeeId);

    boolean existsByProjectIdAndEmployeeIdAndIsCurrentTrueAndIdNot(
            Long projectId, Long employeeId, Long assignmentId);

    Optional<EmployeeAssignment> findFirstByProjectIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(
            Long projectId, Long employeeId);

    List<EmployeeAssignment> findByProjectIdAndDepartmentIdAndDesignationIdAndIsCurrentTrue(
            Long projectId, Long departmentId, Long designationId);

    boolean existsByEmployeeIdAndIsCurrentTrue(Long employeeId);
}
