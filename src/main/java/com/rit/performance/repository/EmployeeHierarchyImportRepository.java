package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeHierarchyImport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EmployeeHierarchyImportRepository extends JpaRepository<EmployeeHierarchyImport, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from EmployeeHierarchyImport i where i.id = :id")
    Optional<EmployeeHierarchyImport> findForCommit(@Param("id") String id);
}
