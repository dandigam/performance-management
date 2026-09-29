package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeOnboardingRepository extends JpaRepository<EmployeeOnboarding, Long> {
    @org.springframework.data.jpa.repository.Query("select o from EmployeeOnboarding o join fetch o.employee where (:status is null or o.status = :status)")
    org.springframework.data.domain.Page<EmployeeOnboarding> findQueue(
            @org.springframework.data.repository.query.Param("status") String status,
            org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from EmployeeOnboarding o where o.employee.id = :employeeId")
    Optional<EmployeeOnboarding> findForUpdateByEmployeeId(@org.springframework.data.repository.query.Param("employeeId") Long employeeId);
    Optional<EmployeeOnboarding> findByEmployeeId(Long employeeId);
    Optional<EmployeeOnboarding> findByRequestActorAndIdempotencyKey(String actor, String key);
}
