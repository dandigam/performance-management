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

    Optional<EmployeeAssignment> findFirstByEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
            Long employeeId, String status);

    default Optional<EmployeeAssignment> findActiveByEmployeeId(Long employeeId) {
        return findFirstByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrueOrderByEffectiveFromDescIdDesc(
                employeeId, "ACTIVE")
                .or(() -> findFirstByEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
                        employeeId, "ACTIVE"));
    }

    Optional<EmployeeAssignment> findFirstByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrueOrderByEffectiveFromDescIdDesc(
            Long employeeId, String status);

    List<EmployeeAssignment> findAllByEmployeeIdAndStatusIgnoreCase(Long employeeId, String status);

    boolean existsByEmployeeIdAndStatusIgnoreCaseAndIsPrimaryAssignmentTrue(Long employeeId, String status);

    @Query("""
            select assignment from EmployeeAssignment assignment
            where assignment.employeeId = :employeeId
              and assignment.effectiveFrom <= :onDate
              and (assignment.effectiveTo is null or assignment.effectiveTo >= :onDate)
            order by assignment.effectiveFrom desc
            """)
    List<EmployeeAssignment> findEffectiveOnDate(@Param("employeeId") Long employeeId,
            @Param("onDate") java.time.LocalDate onDate);

    List<EmployeeAssignment> findByStatusIgnoreCase(String status);

    List<EmployeeAssignment> findByStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDesc(String status);

    List<EmployeeAssignment> findByManagerId(Long managerId);

    List<EmployeeAssignment> findByManagerIdAndStatusIgnoreCase(Long managerId, String status);

    List<EmployeeAssignment> findByLeadId(Long leadId);

    List<EmployeeAssignment> findByLeadIdAndStatusIgnoreCase(Long leadId, String status);

    List<EmployeeAssignment> findByDepartmentId(Long departmentId);

    List<EmployeeAssignment> findByDepartmentIdInAndStatusIgnoreCase(List<Long> departmentIds, String status);

    List<EmployeeAssignment> findByDesignationIdInAndStatusIgnoreCase(List<Long> designationIds, String status);

    Optional<EmployeeAssignment> findFirstBySowIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(
            Long sowId, Long employeeId);

    Optional<EmployeeAssignment> findFirstBySowIdAndEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
            Long sowId, Long employeeId, String status);

    Page<EmployeeAssignment> findBySowIdAndStatusIgnoreCase(Long sowId, String status, Pageable pageable);

    List<EmployeeAssignment> findBySowIdAndStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDescIdDesc(
            Long sowId, String status);

    boolean existsBySowIdAndEmployeeIdAndStatusIgnoreCase(Long sowId, Long employeeId, String status);

    boolean existsBySowIdAndEmployeeIdAndStatusIgnoreCaseAndIdNot(
            Long sowId, Long employeeId, String status, Long assignmentId);

    List<EmployeeAssignment> findBySowIdAndDepartmentIdAndDesignationIdAndStatusIgnoreCase(
            Long sowId, Long departmentId, Long designationId, String status);

    boolean existsByEmployeeIdAndStatusIgnoreCase(Long employeeId, String status);
}
