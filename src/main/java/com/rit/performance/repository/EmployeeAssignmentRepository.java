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

    @Query("""
            select assignment from EmployeeAssignment assignment
            where assignment.employeeId in :employeeIds
              and upper(assignment.status) = 'ASSIGNED'
              and assignment.effectiveFrom <= :onDate
              and (assignment.effectiveTo is null or assignment.effectiveTo >= :onDate)
            order by assignment.effectiveFrom desc, assignment.id desc
            """)
    List<EmployeeAssignment> findCurrentForEmployees(@Param("employeeIds") List<Long> employeeIds,
            @Param("onDate") java.time.LocalDate onDate);

    List<EmployeeAssignment> findByEmployeeId(Long employeeId);


    Optional<EmployeeAssignment> findFirstByEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
            Long employeeId, String status);

    default Optional<EmployeeAssignment> findActiveByEmployeeId(Long employeeId) {
        return findFirstByEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(employeeId, "ACTIVE");
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

    List<EmployeeAssignment> findByStatusIgnoreCase(String status);

    List<EmployeeAssignment> findByStatusIgnoreCaseOrderByEffectiveFromDesc(String status);


    List<EmployeeAssignment> findByManagerIdAndStatusIgnoreCase(Long managerId, String status);


    List<EmployeeAssignment> findByLeadIdAndStatusIgnoreCase(Long leadId, String status);


    @Query("""
            select assignment from EmployeeAssignment assignment
            join Sow sow on sow.id = assignment.sowId
            where sow.businessUnit.id in :departmentIds and upper(assignment.status) = upper(:status)
            """)
    List<EmployeeAssignment> findByDepartmentIdInAndStatusIgnoreCase(List<Long> departmentIds, String status);

    @Query("""
            select distinct assignment.employeeId from EmployeeAssignment assignment
            left join Sow sow on sow.id = assignment.sowId
            where upper(assignment.status) = 'ACTIVE'
              and (:sowId is null or assignment.sowId = :sowId)
              and (:departmentId is null or sow.businessUnit.id = :departmentId)
              and (:designationId is null or exists (
                  select detail.id from SowMilestonePositionAssignment detail
                  where detail.employeeAssignment.id = assignment.id
                    and upper(detail.status) = 'ASSIGNED'
                    and detail.milestonePosition.position.id = :designationId))
            """)
    List<Long> findReportingEmployeeIds(@Param("sowId") Long sowId,
            @Param("departmentId") Long departmentId, @Param("designationId") Long designationId);


    Optional<EmployeeAssignment> findFirstBySowIdAndEmployeeIdOrderByEffectiveFromDescIdDesc(
            Long sowId, Long employeeId);

    Optional<EmployeeAssignment> findFirstBySowIdAndEmployeeIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
            Long sowId, Long employeeId, String status);

    Page<EmployeeAssignment> findBySowIdAndStatusIgnoreCase(Long sowId, String status, Pageable pageable);

    List<EmployeeAssignment> findBySowIdAndStatusIgnoreCaseOrderByEffectiveFromDescIdDesc(
            Long sowId, String status);

    boolean existsBySowIdAndEmployeeIdAndStatusIgnoreCase(Long sowId, Long employeeId, String status);

    boolean existsBySowIdAndEmployeeIdAndStatusIgnoreCaseAndIdNot(
            Long sowId, Long employeeId, String status, Long assignmentId);


}
