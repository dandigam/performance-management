package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, Long> {

    List<EmployeeAssignment> findByEmployeeId(Long employeeId);

    Optional<EmployeeAssignment> findByEmployeeIdAndIsCurrentTrue(Long employeeId);

    List<EmployeeAssignment> findByIsCurrentTrue();

    List<EmployeeAssignment> findByManagerId(Long managerId);

    List<EmployeeAssignment> findByManagerIdAndIsCurrentTrue(Long managerId);

    List<EmployeeAssignment> findByLeadId(Long leadId);

    List<EmployeeAssignment> findByLeadIdAndIsCurrentTrue(Long leadId);

    List<EmployeeAssignment> findByDepartmentId(Long departmentId);

    List<EmployeeAssignment> findByDepartmentIdInAndIsCurrentTrue(List<Long> departmentIds);

    List<EmployeeAssignment> findByDesignationIdInAndIsCurrentTrue(List<Long> designationIds);

    List<EmployeeAssignment> findByProjectId(Long projectId);

    List<EmployeeAssignment> findByProjectIdAndDepartmentIdAndDesignationIdAndIsCurrentTrue(
            Long projectId, Long departmentId, Long designationId);

    boolean existsByEmployeeIdAndIsCurrentTrue(Long employeeId);
}
